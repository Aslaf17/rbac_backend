package com.rbac.service.batch;

import com.rbac.model.batch.Counter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class BatchIdGeneratorService {

    private final MongoTemplate mongoTemplate;

    public String generate(String prefix, String fallbackSeedForPrefix) {
        String normalizedPrefix = normalizePrefix(
                StringUtils.hasText(prefix) ? prefix : fallbackSeedForPrefix);

        int year = Year.now().getValue();
        String counterKey = normalizedPrefix + "-" + year;

        Query query = new Query(Criteria.where("_id").is(counterKey));
        Update update = new Update().inc("seq", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        Counter counter = mongoTemplate.findAndModify(query, update, options, Counter.class);
        long seq = counter != null ? counter.getSeq() : 1L;

        return String.format("%s-%d-%03d", normalizedPrefix, year, seq);
    }

    private String normalizePrefix(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "BATCH";
        }
        String cleaned = raw.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleaned.isEmpty()) {
            return "BATCH";
        }
        return cleaned.length() > 10 ? cleaned.substring(0, 10) : cleaned;
    }
}
