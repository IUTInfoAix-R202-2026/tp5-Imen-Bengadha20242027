package fr.univ_amu.iut.exercice7;

import com.google.inject.Inject;
import fr.univ_amu.iut.exercice4.Site;
import fr.univ_amu.iut.exercice4.SiteDao;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/** ViewModel du capstone : la liste des sites VigieChiro, persistée en base. */
public class SitesViewModel {

  private final SiteDao dao;
  private final ObservableList<Site> sites = FXCollections.observableArrayList();
  private final StringProperty resume = new SimpleStringProperty();

  @Inject
  public SitesViewModel(SiteDao dao) {
    this.dao = dao;

    // Geste 1 : Remplir la liste depuis la base SQLite au démarrage
    sites.setAll(dao.findAll());

    // Geste 1 suite : Lier le résumé au nombre de sites au format attendu par le
    // test
    resume.bind(Bindings.concat(Bindings.size(sites), " site(s) suivi(s)"));
  }

  public ObservableList<Site> sitesProperty() {
    return sites;
  }

  public ReadOnlyStringProperty resumeProperty() {
    return resume;
  }

  /** Persiste un nouveau site puis l'ajoute à la liste observable. */
  public void ajouterCommand(Site site) {
    // Geste 2 : Insertion en base de données, puis mise à jour de la liste de
    // l'écran
    dao.insert(site);
    sites.add(site);
  }

  /** Supprime un site de la base puis de la liste observable. */
  public void supprimerCommand(Site site) {
    // Geste 3 : Suppression en base de données, puis retrait de la liste de l'écran
    dao.delete(site.numeroCarre());
    sites.remove(site);
  }
}
