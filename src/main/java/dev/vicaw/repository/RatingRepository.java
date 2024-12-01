package dev.vicaw.repository;

import java.util.Map;
import java.util.Optional;

import dev.vicaw.model.Rating;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RatingRepository implements PanacheRepository<Rating> {

    private static final String QUERY_BY_RECIPE_ID = "recipe.id = ?1";
    private static final String QUERY_BY_USER_AND_RECIPE_ID = "user.id = :userId and recipe.id = :recipeId";

    private static final String ORDER_BY_HIGHEST_SCORE = "highest";
    private static final String ORDER_BY_LOWEST_SCORE = "lowest";
    private static final String ORDER_BY_RECENTLY_CREATED = "recent";

    private static final Map<String, Sort> SORT_OPTIONS = Map.of(
            ORDER_BY_HIGHEST_SCORE, Sort.by("score").descending(),
            ORDER_BY_LOWEST_SCORE, Sort.by("score").ascending(),
            ORDER_BY_RECENTLY_CREATED, Sort.by("createdAt").descending());

    public PanacheQuery<Rating> listAllRecipeRatings(Long recipeId, String orderBy) {
        Sort sort = SORT_OPTIONS.getOrDefault(orderBy, SORT_OPTIONS.get(ORDER_BY_RECENTLY_CREATED));
        return find(QUERY_BY_RECIPE_ID, sort, recipeId);
    }

    public Optional<Rating> getUserRating(Long userId, Long recipeId) {
        return find(QUERY_BY_USER_AND_RECIPE_ID,
                Parameters.with("userId", userId).and("recipeId", recipeId))
                .firstResultOptional();
    }

    public boolean existsByUserAndRecipeId(Long userId, Long recipeId) {
        return getUserRating(userId, recipeId).isPresent();
    }

    public Long ratingCount(Long recipeId) {
        return find(QUERY_BY_RECIPE_ID, recipeId).count();
    }

    public Double calculateAverageScore(Long recipeId) {
        if (ratingCount(recipeId) == 0)
            return 0.0;

        return getEntityManager().createQuery(
                "select avg(r.score) from Rating r where r.recipe.id = :recipeId", Double.class)
                .setParameter("recipeId", recipeId)
                .getSingleResult();
    }
}