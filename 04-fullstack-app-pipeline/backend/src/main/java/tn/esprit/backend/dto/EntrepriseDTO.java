package tn.esprit.backend.dto;

/** Donnees acceptees en ecriture pour une entreprise (evite le mass assignment). */
public record EntrepriseDTO(Long id, String nom, String adresse) {
}
