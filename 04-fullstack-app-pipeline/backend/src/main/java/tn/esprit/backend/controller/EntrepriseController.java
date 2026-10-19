package tn.esprit.backend.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.backend.dto.EntrepriseDTO;
import tn.esprit.backend.entity.Entreprise;
import tn.esprit.backend.service.IEntrepriseService;

import java.util.List;

@RestController
@RequestMapping("/entreprise")
@AllArgsConstructor
@CrossOrigin("*")
public class EntrepriseController {

    IEntrepriseService entrepriseService;

    @PostMapping("/add")
    public Entreprise addEntreprise(@RequestBody EntrepriseDTO dto) {
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(dto.nom());
        entreprise.setAdresse(dto.adresse());
        return entrepriseService.addEntreprise(entreprise);
    }

    @PutMapping("/update")
    public Entreprise updateEntreprise(@RequestBody EntrepriseDTO dto) {
        Entreprise existing = dto.id() == null ? null : entrepriseService.getEntrepriseById(dto.id());
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable");
        }
        existing.setNom(dto.nom());
        existing.setAdresse(dto.adresse());
        return entrepriseService.updateEntreprise(existing);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteEntreprise(@PathVariable Long id) {
        entrepriseService.deleteEntreprise(id);
    }

    @GetMapping("/get/{id}")
    public Entreprise getEntrepriseById(@PathVariable Long id) {
        return entrepriseService.getEntrepriseById(id);
    }

    @GetMapping("/all")
    public List<Entreprise> getAllEntreprises() {
        return entrepriseService.getAllEntreprises();
    }
}
