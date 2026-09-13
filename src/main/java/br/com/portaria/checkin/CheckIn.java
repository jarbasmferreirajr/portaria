package br.com.portaria.checkin;

import java.time.LocalDateTime;
import java.time.ZoneId;

import br.com.portaria.inscricao.Inscricao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * O registro de que uma pessoa inscrita efetivamente entrou no evento.
 *
 * Uma inscrição tem no máximo um check-in -- garantia expressa como UNIQUE na
 * tabela, não apenas como regra no código.
 */
@Entity
@Table(name = "check_in")
public class CheckIn {

    /**
     * O evento acontece em horário de Brasília, mas o servidor pode não estar
     * nele -- plataformas de implantação costumam rodar em UTC. Sem o fuso
     * explícito, um check-in das 20h seria gravado como 23h.
     */
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscricao_id", nullable = false, unique = true)
    private Inscricao inscricao;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm = LocalDateTime.now(FUSO);

    @Column(name = "registrado_por", nullable = false, length = 80)
    private String registradoPor;

    protected CheckIn() {
        // Exigido pela JPA; não usar diretamente.
    }

    public CheckIn(Inscricao inscricao, String registradoPor) {
        this.inscricao = inscricao;
        this.registradoPor = registradoPor;
        this.registradoEm = LocalDateTime.now(FUSO);
    }

    public Long getId() { return id; }
    public Inscricao getInscricao() { return inscricao; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public String getRegistradoPor() { return registradoPor; }
}