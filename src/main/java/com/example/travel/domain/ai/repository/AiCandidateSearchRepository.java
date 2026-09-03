package com.example.travel.domain.ai.repository;

import com.example.travel.domain.ai.entity.AiJobCandidate;
import com.example.travel.domain.ai.entity.AiTourPlaceCandidate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AiCandidateSearchRepository {
    private final JdbcTemplate jdbcTemplate;
    private final AiJobCandidateRepository jobRepository;
    private final AiTourPlaceCandidateRepository placeRepository;

    public AiCandidateSearchRepository(JdbcTemplate jdbcTemplate,
                                       AiJobCandidateRepository jobRepository,
                                       AiTourPlaceCandidateRepository placeRepository) {
        this.jdbcTemplate = jdbcTemplate; this.jobRepository = jobRepository; this.placeRepository = placeRepository;
    }

    public List<JobMatch> findJobs(Long regionId, float[] embedding, int limit) {
        List<ScoredId> scores = query("ai_job_candidates", regionId, embedding, limit,
                "and (deadline >= current_date "
                        + "or (deadline is null and posted_at >= current_date - 180))");
        Map<Long, AiJobCandidate> values = byId(jobRepository.findAllById(ids(scores)));
        return scores.stream().filter(score -> values.containsKey(score.id()))
                .map(score -> new JobMatch(values.get(score.id()), similarity(score.distance()), null, null)).toList();
    }

    public List<JobMatch> findJobsAcrossRegions(float[] embedding, int limit) {
        String sql = "select c.id, c.embedding <=> cast(? as vector) as distance, "
                + "r.id as region_id, r.name as region_name "
                + "from ai_job_candidates c join regions r on r.id = c.region_id "
                + "where c.active = true and c.embedding is not null and r.is_active = true "
                + "and (c.deadline >= current_date "
                + "or (c.deadline is null and c.posted_at >= current_date - 180)) "
                + "order by c.embedding <=> cast(? as vector) limit ?";
        String vector = vectorLiteral(embedding);
        List<ScoredJobId> scores = jdbcTemplate.query(sql,
                (rs, row) -> new ScoredJobId(rs.getLong("id"), rs.getDouble("distance"),
                        rs.getLong("region_id"), rs.getString("region_name")),
                vector, vector, Math.min(Math.max(limit, 1), 100));
        Map<Long, AiJobCandidate> values = byId(jobRepository.findAllById(
                scores.stream().map(ScoredJobId::id).toList()));
        return scores.stream().filter(score -> values.containsKey(score.id()))
                .map(score -> new JobMatch(values.get(score.id()), similarity(score.distance()),
                        score.regionId(), score.regionName()))
                .toList();
    }

    public List<PlaceMatch> findPlaces(Long regionId, float[] embedding, int limit) {
        List<ScoredId> scores = query("ai_tour_place_candidates", regionId, embedding, limit, "");
        Map<Long, AiTourPlaceCandidate> values = byPlaceId(placeRepository.findAllById(ids(scores)));
        return scores.stream().filter(score -> values.containsKey(score.id()))
                .map(score -> new PlaceMatch(values.get(score.id()), similarity(score.distance()))).toList();
    }

    private List<ScoredId> query(String table, Long regionId, float[] embedding, int limit, String extraWhere) {
        String sql = "select id, embedding <=> cast(? as vector) as distance from " + table
                + " where active = true and embedding is not null and region_id = ? " + extraWhere
                + " order by embedding <=> cast(? as vector) limit ?";
        String vector = vectorLiteral(embedding);
        return jdbcTemplate.query(sql, (rs, row) -> new ScoredId(rs.getLong("id"), rs.getDouble("distance")),
                vector, regionId, vector, Math.min(Math.max(limit, 1), 100));
    }

    private String vectorLiteral(float[] values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(Float.toString(values[i]));
        }
        return builder.append(']').toString();
    }

    private double similarity(double distance) { return Math.max(0, Math.min(1, 1 - distance)); }
    private List<Long> ids(List<ScoredId> scores) { return scores.stream().map(ScoredId::id).toList(); }
    private Map<Long, AiJobCandidate> byId(Iterable<AiJobCandidate> values) {
        Map<Long, AiJobCandidate> result = new HashMap<>(); values.forEach(value -> result.put(value.getId(), value)); return result;
    }
    private Map<Long, AiTourPlaceCandidate> byPlaceId(Iterable<AiTourPlaceCandidate> values) {
        Map<Long, AiTourPlaceCandidate> result = new HashMap<>(); values.forEach(value -> result.put(value.getId(), value)); return result;
    }

    private record ScoredId(Long id, double distance) {}
    private record ScoredJobId(Long id, double distance, Long regionId, String regionName) {}
    public record JobMatch(AiJobCandidate candidate, double similarity, Long regionId, String regionName) {}
    public record PlaceMatch(AiTourPlaceCandidate candidate, double similarity) {}
}
