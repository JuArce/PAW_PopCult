package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.interfaces.MediaDao;
import ar.edu.itba.paw.models.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class MediaDaoJdbcImpl implements MediaDao {
    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert jdbcInsert;
    private static final RowMapper<Media> ROW_MAPPER =
            (rs, rowNum) -> new Media(
                    rs.getInt("mediaId"),
                    rs.getInt("type"),
                    rs.getString("title"),
                    rs.getString("description"),
                    rs.getString("imageURL"),
                    rs.getInt("length"),
                    rs.getDate("releaseDate"),
                    rs.getInt("seasons"),
                    rs.getInt("country"));

    @Autowired
    public MediaDaoJdbcImpl(final DataSource ds) {
        jdbcTemplate = new JdbcTemplate(ds);
        jdbcInsert = new SimpleJdbcInsert(ds).withTableName("media").usingGeneratedKeyColumns("mediaId");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS media (" +
                "mediaId SERIAL PRIMARY KEY," +
                "type INT," +
                "title VARCHAR(100), " +
                "description TEXT," +
                "image TEXT," +
                "length INT," +
                "releaseDate DATE," +
                "seasons INT," +
                "country INT)");
    }

    @Override
    public Optional<Media> getById(int mediaId) {
        return jdbcTemplate.query("SELECT * FROM media WHERE mediaId = ?", new Object[]{mediaId}, ROW_MAPPER)
                .stream().findFirst();
    }

    @Override
    public Optional<List<Media>> getMediaList() {
        return Optional.empty();
    }
}
