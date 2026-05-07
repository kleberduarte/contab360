package com.contabilidade.pj.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    @Query("SELECT ps FROM PushSubscription ps JOIN FETCH ps.usuario WHERE ps.usuario.id = :usuarioId")
    List<PushSubscription> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpoint(String endpoint);
    void deleteByUsuario_Id(Long usuarioId);

    /** Subscriptions de todos os clientes vinculados a uma empresa. */
    @Query("SELECT ps FROM PushSubscription ps WHERE ps.usuario.empresa.id = :empresaId AND ps.usuario.perfil = 'CLIENTE'")
    List<PushSubscription> findByEmpresaId(@Param("empresaId") Long empresaId);

    /**
     * Clientes que devem receber push quando há novas pendências da empresa {@code empresaId}:
     * usuário com {@code empresa} igual, ou só PF cujo CPF bate com {@code Empresa.cpfResponsavel}.
     */
    @Query(
            "SELECT DISTINCT ps FROM PushSubscription ps JOIN FETCH ps.usuario u "
                    + "LEFT JOIN u.empresa ue LEFT JOIN u.clientePessoaFisica cpf "
                    + "WHERE u.perfil = 'CLIENTE' AND (ue.id = :empresaId OR (cpf IS NOT NULL AND EXISTS "
                    + "(SELECT 1 FROM Empresa e WHERE e.id = :empresaId AND e.cpfResponsavel IS NOT NULL "
                    + "AND e.cpfResponsavel = cpf.cpf)))")
    List<PushSubscription> findForEmpresaTomador(@Param("empresaId") Long empresaId);

    /** Subscriptions de todos os clientes vinculados a um ClientePessoaFisica. */
    @Query(
            "SELECT ps FROM PushSubscription ps JOIN FETCH ps.usuario u "
                    + "WHERE u.clientePessoaFisica.id = :clientePfId AND u.perfil = 'CLIENTE'")
    List<PushSubscription> findByClientePessoaFisicaId(@Param("clientePfId") Long clientePfId);

    /** Subscriptions de todos os usuários com perfil CLIENTE. */
    @Query("SELECT ps FROM PushSubscription ps JOIN FETCH ps.usuario u WHERE u.perfil = 'CLIENTE'")
    List<PushSubscription> findAllClientes();
}
