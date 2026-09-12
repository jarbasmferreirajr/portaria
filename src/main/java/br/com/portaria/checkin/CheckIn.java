package br.com.portaria.checkin;

import br.com.portaria.inscricao.Inscricao;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * O registro de que uma pessoa inscrita efetivamente entrou no evento.
 *
 * Uma inscrição tem no máximo um check-in -- garantia expressa como UNIQUE na
 * tabela, não apenas como regra no código.
 */
@Entity
@Table(name = "check_in")
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inscricao_id", nullable = false, unique = true)
    private Inscricao inscricao;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm = LocalDateTime.now();

    @Column(name = "registrado_por", nullable = false, length = 80)
    private String registradoPor;

    protected CheckIn() {
        // Exigido pela JPA; não usar diretamente.
    }

    public CheckIn(Inscricao inscricao, String registradoPor) {
        this.inscricao = inscricao;
        this.registradoPor = registradoPor;
        this.registradoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Inscricao getInscricao() { return inscricao; }
    public LocalDateTime getRegistradoEm() { return registradoEm; }
    public String getRegistradoPor() { return registradoPor; }
}
