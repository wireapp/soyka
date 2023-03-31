package com.wire.integrations.outlook;

import com.wire.integrations.outlook.models.Session;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SessionsDAO {
    @SqlUpdate("INSERT INTO Sessions (session_id, access_token, refresh_token) " +
            "VALUES (:sessionId, :token, :refresh) " +
            "ON CONFLICT (session_id) " +
            "DO UPDATE SET access_token = EXCLUDED.access_token, refresh_token = EXCLUDED.refresh_token")
    int upsert(@Bind("sessionId") String sessionId,
               @Bind("token") String token,
               @Bind("refresh") String refresh);

    @SqlUpdate("DELETE FROM Sessions WHERE session_id = :sessionId")
    int deleteSession(@Bind("sessionId") String sessionId);

    @SqlQuery("SELECT * FROM Sessions WHERE session_id = :sessionId")
    @RegisterRowMapper(_Mapper.class)
    Session get(@Bind("sessionId") String sessionId);

    @SqlUpdate("INSERT INTO State (state_id, verifier) " +
            "VALUES (:stateId, :verifier)")
    int insertState(@Bind("stateId") String stateId,
                    @Bind("verifier") String verifier);

    @SqlQuery("SELECT verifier FROM State WHERE state_id = :stateId")
    String getState(@Bind("stateId") String stateId);

    class _Mapper implements RowMapper<Session> {
        @Override
        public Session map(ResultSet resultSet, StatementContext ctx) throws SQLException {
            Session ret = new Session();
            ret.id = resultSet.getString("session_id");
            ret.token = resultSet.getString("access_token");
            ret.refresh = resultSet.getString("refresh_token");

            return ret;
        }
    }
}
