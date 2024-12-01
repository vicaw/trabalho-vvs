package dev.vicaw.exception;

public class RecipeNotFoundException extends ApiException {
    public static final String ERROR_MESSAGE = "Não existe nenhuma receita com o ID informado.";

    public RecipeNotFoundException() {
        super(404, ERROR_MESSAGE);
    }
}