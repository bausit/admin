package org.bausit.admin.repositories;

import com.querydsl.core.types.dsl.StringPath;
import org.bausit.admin.models.Participant;
import org.bausit.admin.models.QParticipant;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberRepository extends CrudRepository<Participant, Long>,
    QuerydslPredicateExecutor<Participant>,
    QuerydslBinderCustomizer<QParticipant> {

    List<Participant> findByEmail(String emaiil);

    @Override
    default void customize(QuerydslBindings bindings, QParticipant member) {
        bindings.bind(String.class)
            .first((StringPath path, String value) -> path.containsIgnoreCase(value));
        bindings.excluding(member.password);
    }
}
