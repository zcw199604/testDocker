package com.example.bizservice.service;

import com.example.bizservice.model.DependenciesResponse;
import com.example.bizservice.model.DependencyStatus;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

/**
 * Validates PostgreSQL and Redis availability for runtime endpoints.
 */
@Service
public class DependencyStatusService {

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final String datasourceUrl;
    private final String redisHost;
    private final int redisPort;

    /**
     * Creates a dependency validation service.
     *
     * @param dataSourceProvider datasource provider.
     * @param redisConnectionFactoryProvider redis connection factory provider.
     * @param datasourceUrl configured datasource URL.
     * @param redisHost configured redis host.
     * @param redisPort configured redis port.
     */
    public DependencyStatusService(
            ObjectProvider<DataSource> dataSourceProvider,
            ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort) {
        this.dataSourceProvider = dataSourceProvider;
        this.redisConnectionFactoryProvider = redisConnectionFactoryProvider;
        this.datasourceUrl = datasourceUrl;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    /**
     * Returns aggregated dependency validation data.
     *
     * @return dependencies response.
     */
    public DependenciesResponse inspectDependencies() {
        DependencyStatus postgres = getPostgresStatus();
        DependencyStatus redis = getRedisStatus();
        String overallStatus = postgres.available() && redis.available() ? "UP" : "DEGRADED";
        return new DependenciesResponse(overallStatus, postgres, redis, Instant.now());
    }

    /**
     * Checks PostgreSQL connectivity.
     *
     * @return PostgreSQL dependency status.
     */
    public DependencyStatus getPostgresStatus() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("url", sanitizeJdbcUrl(datasourceUrl));
        if (dataSource == null) {
            return unavailable("postgres", "PostgreSQL datasource is not configured.", details);
        }

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String databaseName = readDatabaseName(connection);
            if (databaseName != null && !databaseName.isBlank()) {
                details.put("database", databaseName);
            }
            details.put("product", metadata.getDatabaseProductName());
            details.put("productVersion", metadata.getDatabaseProductVersion());
            return new DependencyStatus(
                    "postgres",
                    true,
                    "PostgreSQL connection is available.",
                    details);
        } catch (Exception exception) {
            return unavailable("postgres", resolveMessage(exception), details);
        }
    }

    /**
     * Checks Redis connectivity.
     *
     * @return Redis dependency status.
     */
    public DependencyStatus getRedisStatus() {
        RedisConnectionFactory connectionFactory = redisConnectionFactoryProvider.getIfAvailable();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("host", redisHost);
        details.put("port", String.valueOf(redisPort));
        if (connectionFactory == null) {
            return unavailable("redis", "Redis connection factory is not configured.", details);
        }

        try (RedisConnection connection = connectionFactory.getConnection()) {
            String ping = connection.ping();
            details.put("ping", ping == null ? "" : ping);
            boolean available = "PONG".equalsIgnoreCase(ping);
            if (available) {
                return new DependencyStatus(
                        "redis",
                        true,
                        "Redis connection is available.",
                        details);
            }
            return unavailable("redis", "Redis ping returned an unexpected response.", details);
        } catch (Exception exception) {
            return unavailable("redis", resolveMessage(exception), details);
        }
    }

    private DependencyStatus unavailable(String name, String message, Map<String, String> details) {
        return new DependencyStatus(name, false, message, details);
    }

    private String readDatabaseName(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("SELECT current_database()");
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
            return "";
        }
    }

    private String sanitizeJdbcUrl(String url) {
        return url == null ? "" : url.replaceAll("//([^:@/]+):([^@/]+)@", "//$1:***@");
    }

    private String resolveMessage(Exception exception) {
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }
}
