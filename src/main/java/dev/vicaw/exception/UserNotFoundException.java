package dev.vicaw.exception;

public class UserNotFoundException extends ApiException {
    public static final String ERROR_MESSAGE = "Não existe nenhum usuário com o ID informado.";

    public UserNotFoundException() {
        super(404, ERROR_MESSAGE);
    }
}
