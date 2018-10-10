package starter.jdbi;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.Results;
import org.jooby.Status;
import org.jooby.jdbc.Jdbc;
import org.jooby.jdbi.Jdbi3;
import org.jooby.jdbi.TransactionalRequest;
import org.jooby.json.Jackson;

/**
 * jdbi starter project:
 */
public class App extends Jooby {

    {
        use(new Jackson());

        use(new Jdbc());

        use(new Jdbi3()
                /** Install SqlObjectPlugin */
                .doWith(jdbi -> {
                    jdbi.installPlugin(new SqlObjectPlugin());
                })
                /** Creates a transaction per request and attach PetRepository */
                .transactionPerRequest(
                        new TransactionalRequest()
                                .attach(PetRepository.class)
                )
        );

        /** Create table + pets: */
        onStart(() -> {
            Jdbi jdbi = require(Jdbi.class);
            jdbi.useHandle(h -> {
                h.createUpdate("create table pets (id bigint auto_increment, name varchar(255))")
                        .execute();

                PetRepository repo = h.attach(PetRepository.class);
                repo.insert(new Pet(1L, "Lala"));
                repo.insert(new Pet(2L, "Mandy"));
                repo.insert(new Pet(3L, "Sasha"));
            });
        });

        /**
         *
         * Everything about your Pets.
         */
        path("/api/pets", () -> {
            /**
             *
             * List pets ordered by id.
             *
             * @param start Start offset, useful for paging. Default is <code>0</code>.
             * @param max Max page size, useful for paging. Default is <code>50</code>.
             * @return Pets ordered by name.
             */
            get(req -> {
                PetRepository db = require(PetRepository.class);
                int start = req.param("start").intValue(0);
                int max = req.param("max").intValue(20);

                return db.list(start, max);
            });

            /**
             *
             * Find pet by ID
             *
             * @param id Pet ID.
             * @return Returns <code>200</code> with a single pet or <code>404</code>
             */
            get("/:id", req -> {
                PetRepository db = require(PetRepository.class);

                long id = req.param("id").longValue();

                Pet pet = db.findById(id);
                if (pet == null) {
                    throw new Err(Status.NOT_FOUND);
                }
                return pet;
            });

            /**
             *
             * Add a new pet to the store.
             *
             * @param body Pet object that needs to be added to the store.
             * @return Returns a saved pet.
             */
            post(req -> {
                PetRepository db = require(PetRepository.class);

                Pet pet = req.body(Pet.class);

                long id = db.insert(pet);
                return new Pet(id, pet.getName());
            });

            /**
             *
             * Update an existing pet.
             *
             * @param body Pet object that needs to be updated.
             * @return Returns a saved pet.
             */
            put(req -> {
                PetRepository db = require(PetRepository.class);

                Pet pet = req.body(Pet.class);
                if (!db.update(pet)) {
                    throw new Err(Status.NOT_FOUND);
                }
                return pet;
            });

            /**
             *
             * Deletes a pet by ID.
             *
             * @param id Pet ID.
             * @return A <code>204</code>
             */
            delete("/:id", req -> {
                PetRepository db = require(PetRepository.class);

                long id = req.param("id").longValue();

                if (!db.delete(id)) {
                    throw new Err(Status.NOT_FOUND);
                }
                return Results.noContent();
            });
        });
    }

    public static void main(final String[] args) {
        run(App::new, args);
    }

}
