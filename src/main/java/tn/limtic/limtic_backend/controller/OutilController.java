package tn.limtic.limtic_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.limtic.limtic_backend.model.Outil;
import tn.limtic.limtic_backend.service.OutilService;

@RestController
@RequestMapping("/api/outils")
public class OutilController {

    private final OutilService outilService;

    public OutilController(OutilService outilService) {
        this.outilService = outilService;
    }

    @GetMapping
    public List<Outil> getAll() {
        return outilService.getAll();
    }

    @GetMapping("/{id}")
    public Outil getById(@PathVariable Long id) {
        return outilService.getById(id);
    }

    @PostMapping
    public Outil create(@RequestBody Outil outil) {
        return outilService.save(outil);
    }

    @PutMapping("/{id}")
    public Outil update(@PathVariable Long id, @RequestBody Outil outil) {
        outil.setId(id);
        return outilService.save(outil);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        outilService.delete(id);
    }
}