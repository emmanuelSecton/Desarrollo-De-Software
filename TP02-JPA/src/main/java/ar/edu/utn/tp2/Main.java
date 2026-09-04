package ar.edu.utn.tp2;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class Main {
    public static void main(String[] args) {
        try (EntityManagerFactory entityManagerFactory =
                     Persistence.createEntityManagerFactory("tp2-jpa")) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            entityManager.getTransaction().begin();
            Alumno alumno = new Alumno("Ada", "Lovelace");
            entityManager.persist(alumno);
            entityManager.getTransaction().commit();

            System.out.printf("Alumno persistido con id %d: %s %s%n",
                    alumno.getId(), alumno.getNombre(), alumno.getApellido());
            entityManager.close();
        }
    }
}
