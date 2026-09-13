package br.com.fiap.notificacaoapi.repository.notificacao;

import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {

    Optional<Notificacao> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Query(value = """
            SELECT n.* FROM notificacao n
            INNER JOIN (
                SELECT agendamento_id, MAX(id) AS max_id
                FROM notificacao
                WHERE tipo_notificacao IN ('CONSULTA_CRIADA', 'CONSULTA_ATUALIZADA')
                GROUP BY agendamento_id
            ) ultimo_estado ON ultimo_estado.agendamento_id = n.agendamento_id AND ultimo_estado.max_id = n.id
            WHERE CAST(n.datahora_consulta AS date) = :data
            """, nativeQuery = true)
    List<Notificacao> findUltimoEstadoPorAgendamentoNaData(@Param("data") LocalDate data);

}
