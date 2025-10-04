package com.wishlist.service;

import com.wishlist.domain.model.DatabaseSequence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@ExtendWith(MockitoExtension.class)
class SequenceGeneratorServiceTest {

    @Mock
    private MongoOperations mongoOperations;

    @InjectMocks
    private SequenceGeneratorService sequenceGeneratorService;

    @Captor
    private ArgumentCaptor<Query> queryCaptor;

    @Captor
    private ArgumentCaptor<Update> updateCaptor;

    @Captor
    private ArgumentCaptor<FindAndModifyOptions> optionsCaptor;

    @BeforeEach
    void setUp() {
        // noop
    }

    @Test
    void generateSequence_returnsSequenceFromDatabase_whenFindAndModifyReturnsSequence() {
        String seqName = "mySeq";
        DatabaseSequence returned = new DatabaseSequence();
        returned.setSeq(42L);

        when(mongoOperations.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(DatabaseSequence.class)))
                .thenReturn(returned);

        long result = sequenceGeneratorService.generateSequence(seqName);

        assertThat(result).isEqualTo(42L);

        verify(mongoOperations).findAndModify(queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(DatabaseSequence.class));

        Query capturedQuery = queryCaptor.getValue();
        Update capturedUpdate = updateCaptor.getValue();
        FindAndModifyOptions capturedOptions = optionsCaptor.getValue();

        // Verifica que a query pesquisa pelo _id correto
        Query expectedQuery = query(where("_id").is(seqName));
        assertThat(capturedQuery.getQueryObject().toJson()).isEqualTo(expectedQuery.getQueryObject().toJson());

        // Verifica que existe incremento de seq por 1
        assertThat(capturedUpdate.getUpdateObject().toJson()).contains("\"$inc\"");

        // Verifica que options pedem retorno do novo documento e upsert=true
        assertThat(capturedOptions.isUpsert()).isTrue();
        assertThat(capturedOptions.isReturnNew()).isTrue();
    }

    @Test
    void generateSequence_returnsOne_whenFindAndModifyReturnsNull() {
        String seqName = "otherSeq";

        when(mongoOperations.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(DatabaseSequence.class)))
                .thenReturn(null);

        long result = sequenceGeneratorService.generateSequence(seqName);

        assertThat(result).isEqualTo(1L);

        verify(mongoOperations).findAndModify(queryCaptor.capture(), updateCaptor.capture(), optionsCaptor.capture(), eq(DatabaseSequence.class));

        Query capturedQuery = queryCaptor.getValue();
        Query expectedQuery = query(where("_id").is(seqName));
        assertThat(capturedQuery.getQueryObject().toJson()).isEqualTo(expectedQuery.getQueryObject().toJson());
    }
}