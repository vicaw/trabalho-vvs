package dev.vicaw.repository;

import dev.vicaw.model.Recipe;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {
    public PanacheQuery<Recipe> listAllRecipes(String orderBy) {
        String jpql = "from Recipe r ";

        switch (orderBy) {
            case "recent":
                jpql += "order by r.createdAt desc";
                break;
            case "highest":
            default:
                jpql += "order by (select coalesce(avg(ra.score), 0) from Rating ra where ra.recipe.id = r.id) desc";
                break;
        }

        return find(jpql);
    }

    public PanacheQuery<Recipe> listAllUserRecipes(Long authorId, String orderBy) {
        String jpql = "from Recipe r where r.user.id = ?1 ";

        switch (orderBy) {
            case "recent":
                jpql += "order by r.createdAt desc";
                break;
            case "highest":
            default:
                jpql += "order by (select coalesce(avg(ra.score), 0) from Rating ra where ra.recipe.id = r.id) desc";
                break;
        }

        return find(jpql, authorId);
    }

    public PanacheQuery<Recipe> search(String query, String orderBy) {
        String jpql = "from Recipe r where CONCAT_WS(' ', r.titulo, r.ingredientes) LIKE CONCAT('%',?1,'%')";

        switch (orderBy) {
            case "older":
                jpql += "order by r.createdAt asc";
                break;
            case "newer":
                jpql += "order by r.createdAt desc";
                break;
            case "rating":
            default:
                jpql += "order by (select coalesce(avg(ra.score), 0) from Rating ra where ra.recipe.id = r.id) desc";
                break;
        }

        return find(jpql, query);
    }
}
