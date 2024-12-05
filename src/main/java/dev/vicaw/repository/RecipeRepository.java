package dev.vicaw.repository;

import java.util.Map;

import dev.vicaw.model.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Page;
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

    public PanacheQuery<Recipe> listRecipes(String orderBy, Integer pageSize, Integer pageNumber) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        PanacheQuery<Recipe> query = findAll(sort);

        if (pageSize != null && pageNumber != null) {
            query = query.page(Page.of(pageNumber, pageSize));
        }

        return query;
    }

    public PanacheQuery<Recipe> listUserRecipes(Long authorId, String orderBy, Integer pageSize, Integer pageNumber) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        PanacheQuery<Recipe> query = find("user.id", sort, authorId);

        if (pageSize != null && pageNumber != null) {
            query = query.page(Page.of(pageNumber, pageSize));
        }

        return query;
    }

    public PanacheQuery<Recipe> search(String query, String orderBy, Integer pageNumber, Integer pageSize) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_HIGHEST_SCORE));
        PanacheQuery<Recipe> panacheQuery = find(
                "CONCAT_WS(' ', LOWER(titulo), LOWER(ingredientes)) LIKE CONCAT('%', LOWER(?1), '%')", sort, query);
        return panacheQuery.page(Page.of(pageNumber, pageSize));
    }

}
