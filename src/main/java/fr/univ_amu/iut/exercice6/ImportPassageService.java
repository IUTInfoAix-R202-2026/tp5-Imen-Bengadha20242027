package fr.univ_amu.iut.exercice6;

import fr.univ_amu.iut.jdbc.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

/**
 * Service d'import d'un passage et de ses observations (exercice 6) : illustration des
 * <b>transactions</b>.
 *
 * <p>Importer un passage et ses observations, c'est plusieurs écritures (un {@code INSERT} dans
 * {@code passage}, puis un {@code INSERT} par observation). Il faut que ce soit <b>tout ou rien</b>
 * : si une observation échoue (par exemple parce que son taxon n'existe pas), on ne veut pas d'un
 * passage à moitié importé. C'est le rôle d'une transaction : on désactive l'auto-commit, on
 * exécute toutes les écritures, on {@code commit()} à la fin ; au moindre problème, on {@code
 * rollback()} pour tout annuler.
 */
public class ImportPassageService {

  private final DataSource source;

  public ImportPassageService(DataSource source) {
    this.source = source;
  }

  /**
   * Importe un passage et ses observations de façon atomique.
   *
   * @return l'identifiant généré du passage inséré
   * @throws DataAccessException si l'import échoue (la base est alors laissée intacte)
   */
  public long importer(
      String numeroCarre,
      String codePoint,
      int numeroPassage,
      int annee,
      List<ObservationAImporter> observations) {

    String sqlPassage =
        """
        INSERT INTO passage (numero_carre, code_point, numero_passage, annee, statut_workflow)
        VALUES (?, ?, ?, ?, 'Importé')
        """;

    String sqlObservation =
        """
        INSERT INTO observation (passage_id, temps_debut, temps_fin, frequence_mediane, code_taxon, probabilite)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    long passageId = -1;
    Connection connexion = null;

    try {
      connexion = source.getConnection();
      connexion.setAutoCommit(false);

      try (PreparedStatement psPassage =
          connexion.prepareStatement(sqlPassage, Statement.RETURN_GENERATED_KEYS)) {
        psPassage.setString(1, numeroCarre);
        psPassage.setString(2, codePoint);
        psPassage.setInt(3, numeroPassage);
        psPassage.setInt(4, annee);
        psPassage.executeUpdate();

        try (ResultSet keys = psPassage.getGeneratedKeys()) {
          if (keys.next()) {
            passageId = keys.getLong(1);
          }
        }
      }

      try (PreparedStatement psObservation = connexion.prepareStatement(sqlObservation)) {
        for (ObservationAImporter obs : observations) {
          psObservation.setLong(1, passageId);
          psObservation.setDouble(2, obs.tempsDebut());
          psObservation.setDouble(3, obs.tempsFin());
          psObservation.setInt(4, obs.frequenceMediane());
          psObservation.setString(5, obs.codeTaxon());
          psObservation.setDouble(6, obs.probabilite());
          psObservation.executeUpdate();
        }
      }

      connexion.commit();

    } catch (SQLException e) {
      annulerSilencieusement(connexion);
      throw new DataAccessException("Erreur lors de l'import du passage", e);
    } finally {
      fermerSilencieusement(connexion);
    }

    return passageId;
  }

  /** Nombre de passages en base (fourni, utile pour vérifier qu'un rollback a bien tout annulé). */
  public int nombrePassages() {
    try (Connection connexion = source.getConnection();
        PreparedStatement ps = connexion.prepareStatement("SELECT COUNT(*) FROM passage");
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getInt(1);
    } catch (SQLException e) {
      throw new DataAccessException("Impossible de compter les passages", e);
    }
  }

  private static void annulerSilencieusement(Connection connexion) {
    if (connexion != null) {
      try {
        connexion.rollback();
      } catch (SQLException ignore) {
        // rien à faire : on remonte déjà l'exception d'origine
      }
    }
  }

  private static void fermerSilencieusement(Connection connexion) {
    if (connexion != null) {
      try {
        connexion.close();
      } catch (SQLException ignore) {
        // connexion déjà fermée ou inutilisable
      }
    }
  }
}
