package tn.esprit.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.backend.dto.EntrepriseDTO;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.service.IEntrepriseService;

class EntrepriseControllerTest {

    private final IEntrepriseService service = mock(IEntrepriseService.class);
    private final EntrepriseController controller = new EntrepriseController(service);

    @Test
    void addIgnoresTheIdSentByTheClient() {
        when(service.addEntreprise(any(Entreprise.class))).thenAnswer(i -> i.getArgument(0));

        Entreprise result = controller.addEntreprise(new EntrepriseDTO(999L, "ESPRIT", "Tunis"));

        assertNull(result.getId());
        assertEquals("ESPRIT", result.getNom());
        assertEquals("Tunis", result.getAdresse());
    }

    @Test
    void updateChangesOnlyNameAndAddress() {
        Entreprise existing = new Entreprise();
        existing.setId(1L);
        existing.setNom("Ancien");
        existing.setAdresse("Sfax");
        when(service.getEntrepriseById(1L)).thenReturn(existing);
        when(service.updateEntreprise(any(Entreprise.class))).thenAnswer(i -> i.getArgument(0));

        Entreprise result = controller.updateEntreprise(new EntrepriseDTO(1L, "Nouveau", "Tunis"));

        assertEquals(1L, result.getId());
        assertEquals("Nouveau", result.getNom());
        assertEquals("Tunis", result.getAdresse());
    }

    @Test
    void updateReturnsNotFoundWhenTheEntrepriseDoesNotExist() {
        when(service.getEntrepriseById(5L)).thenReturn(null);

        EntrepriseDTO dto = new EntrepriseDTO(5L, "X", "Y");

        assertThrows(ResponseStatusException.class, () -> controller.updateEntreprise(dto));
    }
}
