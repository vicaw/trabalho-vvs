package dev.vicaw.repository;

import java.util.Map;

import dev.vicaw.model.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {

    private static final String ORDER_BY_HIGHEST_SCORE = "rating";
    private static final String ORDER_BY_RECENT = "recent";
    private static final String ORDER_BY_OLDEST = "oldest";

    private static final Map<String, Sort> SORT_OPTIONS = Map.of(
            ORDER_BY_RECENT, Sort.by("createdAt").descending(),
            ORDER_BY_OLDEST, Sort.by("createdAt").ascending(),
            ORDER_BY_HIGHEST_SCORE,
            Sort.by("(select coalesce(avg(ra.score), 0) from Rating ra where ra.recipe.id = r.id)").descending());

    public PanacheQuery<Recipe> listAllRecipes(String orderBy) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        return findAll(sort);
    }

    public PanacheQuery<Recipe> listAllUserRecipes(Long authorId, String orderBy) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        return find("user.id", sort, authorId);
    }

    public PanacheQuery<Recipe> search(String query, String orderBy) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        return find("CONCAT_WS(' ', titulo, ingredientes) LIKE CONCAT('%', ?1, '%')", sort, query);
    }

}
