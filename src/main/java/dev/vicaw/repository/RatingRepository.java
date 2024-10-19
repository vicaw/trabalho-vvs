package dev.vicaw.repository;

import java.util.Optional;

import dev.vicaw.model.Rating;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

@ApplicationScoped
public class RatingRepository implements PanacheRepository<Rating> {
    public PanacheQuery<Rating> listAllRecipeRatings(Long recipeId, String orderBy) {

        Sort sort;

        switch (orderBy) {

            case "highest":
                sort = Sort.by("score").descending();
                break;
            case "lowest":
                sort = Sort.by("score").ascending();
                break;
            default:

                sort = Sort.by("createdAt").descending();
                break;

        }

        return find("recipe.id = ?1", sort, recipeId);
    }

    public Optional<Rating> getUserRating(Long userId, Long recipeId) {
        return find("user.id = :userId and recipe.id = :recipeId",
                Parameters.with("userId", userId).and("recipeId", recipeId))
                .firstResultOptional();
    }

    public boolean existsByUserAndRecipeId(Long userId, Long recipeId) {
        return find("user.id = :userId and recipe.id = :recipeId",
                Parameters.with("userId", userId).and("recipeId", recipeId))
                .firstResultOptional()
                .isPresent();
    }

    public Double calculateAverageScore(Long recipeId) {
        Long count = find("recipe.id", recipeId).count();
        if (count == 0) {
            return 0.0;
        }

        TypedQuery<Double> query = getEntityManager().createQuery(
                "select avg(r.score) from Rating r where r.recipe.id = :recipeId", Double.class);

        query.setParameter("recipeId", recipeId);

        Double avg = query.getSingleResult();

        return avg;

    }

    public Long ratingCount(Long recipeId) {
        return find("recipe.id", recipeId).count();
    }

}
