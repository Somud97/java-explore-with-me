package ru.practicum.statsserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.statsserver.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT h.app, h.uri, COUNT(h) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<Object[]> findStats(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    @Query("SELECT h.app, h.uri, COUNT(DISTINCT h.ip) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<Object[]> findUniqueStats(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("SELECT h.app, h.uri, COUNT(h) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(h) DESC")
    List<Object[]> findStatsForUris(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("uris") List<String> uris);

    @Query("SELECT h.app, h.uri, COUNT(DISTINCT h.ip) " +
            "FROM EndpointHit h " +
            "WHERE h.timestamp BETWEEN :start AND :end " +
            "AND h.uri IN :uris " +
            "GROUP BY h.app, h.uri " +
            "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<Object[]> findUniqueStatsForUris(@Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end,
                                          @Param("uris") List<String> uris);
}

