package br.com.fiap.notificacaoapi.exceptions;

public class NotificacaoNaoEncontradaException extends RuntimeException {

    public NotificacaoNaoEncontradaException(String message) {
        super(message);
    }
}
