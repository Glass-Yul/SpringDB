package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 트랜잭션 - 파라미터 연동, 풀을 고려한 종료, 리파지토리 커넥션 유지
 */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {
    private final MemberRepositoryV2 memberRepository;
    private final DataSource dataSource;

    public void accountTransfer(String formId, String toId, int money) throws SQLException {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false); // 트랜잭션 시작
            bizLogic(formId, toId, money, con); // 비즈니스 로직
            con.commit(); // 성공 시
        } catch (Exception e) {
            con.rollback(); // 실패 시
            throw new IllegalStateException(e);
        } finally {
            release(con); // 리파지토리가 아닌 서비스에서 커넥션 닫아줌
        }
    }

    private void bizLogic(String formId, String toId, int money, Connection con) throws SQLException {
        Member formMember = memberRepository.findById(con, formId);
        Member tomember = memberRepository.findById(con, toId);

        memberRepository.update(con, formId, formMember.getMoney() - money);
        validation(tomember);
        memberRepository.update(con, toId, tomember.getMoney() + money);
    }

    private void validation(Member toMember) {
        if (toMember.getMemberId().equals("ex")) {
            throw new IllegalStateException("이체 중 예외 발생");
        }
    }
    private void release(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true); //커넥션 풀 고려
                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }
}
