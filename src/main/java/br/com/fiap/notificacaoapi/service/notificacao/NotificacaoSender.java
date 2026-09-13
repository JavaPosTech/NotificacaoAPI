package br.com.fiap.notificacaoapi.service.notificacao;

import br.com.fiap.notificacaoapi.model.entity.notificacao.Notificacao;

public interface NotificacaoSender {

    void enviar(Notificacao notificacao);

}
