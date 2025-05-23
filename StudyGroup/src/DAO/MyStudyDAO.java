package DAO;

import DTO.MyStudyDTO;
import DTO.UserDTO;
import main.AppMain;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyStudyDAO {

    public boolean createStudyGroup(String name, int leaderId, String description, 
                                    Date startDate, Date endDate, String certMethod, int deposit) {

        String insertGroupSQL = "INSERT INTO StudyGroups (name, leader_id, description, start_date, end_date, cert_method, deposit) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertLeaderSQL = "INSERT INTO GroupMembers (study_id, user_id, status) VALUES (?, ?, 'active')";

        try (PreparedStatement groupStmt = AppMain.conn.prepareStatement(insertGroupSQL, Statement.RETURN_GENERATED_KEYS)) {
            // 1. StudyGroups 테이블에 스터디 추가
            groupStmt.setString(1, name);
            groupStmt.setInt(2, leaderId);
            groupStmt.setString(3, description);
            groupStmt.setDate(4, startDate);
            groupStmt.setDate(5, endDate);
            groupStmt.setString(6, certMethod);
            groupStmt.setInt(7, deposit);

            int rows = groupStmt.executeUpdate();

            if (rows > 0) {
                // 2. 생성된 study_id 가져오기
                try (ResultSet rs = groupStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int studyId = rs.getInt(1);

                        // 3. 리더를 GroupMembers에 자동 등록
                        try (PreparedStatement leaderStmt = AppMain.conn.prepareStatement(insertLeaderSQL)) {
                            leaderStmt.setInt(1, studyId);
                            leaderStmt.setInt(2, leaderId);
                            int memberRows = leaderStmt.executeUpdate();

                            return memberRows > 0;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    
    public List<MyStudyDTO> getMyStudies(UserDTO user) {
        List<MyStudyDTO> studyList = new ArrayList<>();
        String sql = "SELECT sg.study_id, sg.name AS study_name, u.user_name AS leader_name, sg.start_date " +
                     "FROM GroupMembers gm " +
                     "JOIN StudyGroups sg ON gm.study_id = sg.study_id " +
                     "JOIN Users u ON sg.leader_id = u.user_id " +
                     "WHERE gm.user_id = ?";

        try (PreparedStatement pstmt = AppMain.conn.prepareStatement(sql)) {
            pstmt.setInt(1, user.getUserId());  // userId 바로 사용
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                MyStudyDTO dto = new MyStudyDTO(
                        rs.getInt("study_id"),
                        rs.getString("study_name"),
                        rs.getString("leader_name"),
                        rs.getDate("start_date")
                );
                studyList.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return studyList;
    }

    public boolean withdrawFromStudy(int studyId, UserDTO user) {
        String sql = "DELETE FROM GroupMembers WHERE study_id = ? AND user_id = ?";
        try (PreparedStatement pstmt = AppMain.conn.prepareStatement(sql)) {
            pstmt.setInt(1, studyId);
            pstmt.setInt(2, user.getUserId());  // userId 바로 사용
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
