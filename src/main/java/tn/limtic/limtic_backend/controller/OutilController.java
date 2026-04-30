package tn.limtic.limtic_backend.controller;

import org.springframework.web.bind.annotation.*;
import tn.limtic.limtic_backend.model.Outil;
import tn.limtic.limtic_backend.service.OutilService;
import java.util.List;

@RestController
@RequestMapping("/api/outils")
@CrossOrigin(origins = {"http://localhost:4200", "https://localhost:4200"}, allowCredentials = "true")
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