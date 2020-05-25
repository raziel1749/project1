package com.freeboard02.domain.board;

import com.freeboard02.api.board.BoardForm;
import com.freeboard02.domain.board.enums.SearchType;
import com.freeboard02.domain.user.UserEntity;
import com.freeboard02.domain.user.UserMapper;
import com.freeboard02.domain.user.enums.UserRole;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"file:src/main/webapp/WEB-INF/applicationContext.xml"})
@Transactional
@Rollback(value = false)
public class BoardMapperTest {

    @Autowired
    private BoardMapper boardMapper;

    @Autowired
    private UserMapper userMapper;

    private UserEntity user;

    private BoardEntity targetBoard;

    final int PAGE = 0;
    final int SIZE = 10;

    @BeforeEach
    public void init() {
        user = userMapper.findAll().get(3);
        targetBoard = BoardEntity.builder()
                .title(randomString())
                .contents(randomString())
                .writer(user).build();
    }

    @Test
    public void mapperInsert() {
        assertThat(targetBoard.getId(), equalTo(0L));
        boardMapper.save(targetBoard);
        assertThat(targetBoard.getId(), not(equalTo(0L)));
    }

    @Test
    public void mapperFindById() {
        boardMapper.save(targetBoard);

        BoardEntity entity = boardMapper.findById(targetBoard.getId()).get();

        assertThat(targetBoard.getTitle(), equalTo(entity.getTitle()));
        assertThat(targetBoard.getContents(), equalTo(entity.getContents()));
        assertThat(entity.getWriter().getId(), equalTo(user.getId()));
        assertThat(entity.getWriter().getRole(), equalTo(user.getRole()));
    }

    @Test
    public void mapperDelete() {
        boardMapper.save(targetBoard);

        long targetId = targetBoard.getId();
        assertThat(targetId, not(equalTo(0L)));

        Optional<BoardEntity> saved = boardMapper.findById(targetId);
        assertThat(saved, OptionalMatchers.isPresent());

        boardMapper.deleteById(targetId);
        Optional<BoardEntity> deleted = boardMapper.findById(targetId);
        assertThat(deleted, OptionalMatchers.isEmpty());
    }

    @Test
    public void mapperUpdate() {
        boardMapper.save(targetBoard);

        BoardForm form = BoardForm.builder().contents(randomString()).title(randomString()).build();
        BoardEntity entity = form.convertBoardEntity(targetBoard.getWriter());
        entity.setId(targetBoard.getId());
        boardMapper.updateById(entity);

        BoardEntity updatedEntity = boardMapper.findById(targetBoard.getId()).get();
        assertThat(updatedEntity.getContents(), equalTo(form.getContents()));
        assertThat(updatedEntity.getTitle(), equalTo(form.getTitle()));
    }

    @Test
    public void mapperPaging() {
        // 현재 시간은 유일하다.
        String time = LocalDateTime.now().toString();

        // assertThat에서 비교를 위해 사용할 저장된 id 리스트이다.
        List<Long> savedEntityIds = new ArrayList<>();

        // 총 20개의 새로운 데이터를 save 할 것이다.
        for (int i = 0; i < 20; ++i) {
            // 현재시간을 contents로 넣는다.
            BoardEntity boardEntity = BoardEntity.builder().writer(user).contents(time).title("title").build();
            boardMapper.save(boardEntity);
            savedEntityIds.add(boardEntity.getId());
            // 저장 후 할당된 id를 savedEntityIds에 추가한다.
        }

        // PAGE = 0, SIZE = 10
        List<BoardEntity> findEntities = boardMapper.findAll(SearchType.CONTENTS.name(), time, PAGE, SIZE);
        // assertThat에서 비교를 위해 위의 findAll에서 얻어온 엔티티의 id 리스트를 생성한다.
        List<Long> findEntityIds = findEntities.stream().map(boardEntity -> boardEntity.getId()).collect(Collectors.toList());

        // 얻어온 엔티티 목록이 SIZE와 일치하는지 확인한다. (limit = paging이 제대로 작동했는지 확인한다.)
        assertThat(findEntities.size(), equalTo(SIZE));
        // 얻어온 엔티티 목록이 방금 save한 데이터 목록의 일부가 맞는지 (20개를 저장하고 10개를 가져왔으므로.) 확인한다.
        assertThat(savedEntityIds, hasItems(findEntityIds.toArray(new Long[SIZE])));
    }

    @Test
    public void mapperFindAllByWriterIn() {
        List<UserEntity> userEntities = userMapper.findAll();
        List<UserEntity> writers = userEntities.subList(userEntities.size() - 4, userEntities.size() - 1);
        List<Long> writerIds = writers.stream().map(writer -> writer.getId()).collect(Collectors.toList());

        List<Long> savedEntityIds = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            // 글 작성자를 번갈아가며 사용하여 새로운 글을 저장한다.
            BoardEntity boardEntity = BoardEntity.builder().writer(writers.get(i % writers.size())).contents("contents").title("title").build();
            boardMapper.save(boardEntity);
            savedEntityIds.add(boardEntity.getId());
            // 저장 후 할당된 id를 savedEntityIds에 추가한다.
        }

        List<BoardEntity> findEntities = boardMapper.findAllByWriterIn(writers, PAGE, SIZE);
        List<Long> findEntityIds = findEntities.stream().map(boardEntity -> boardEntity.getId()).collect(Collectors.toList());
        List<Long> findWriterIds = findEntities.stream().map(boardEntity -> boardEntity.getWriter().getId()).distinct().collect(Collectors.toList());

        assertThat(findEntities.size(), equalTo(SIZE));
        assertThat(savedEntityIds, hasItems(findEntityIds.toArray(new Long[SIZE])));
        assertThat(writerIds, hasItems(findWriterIds.toArray(new Long[findWriterIds.size()])));
    }

    @Test
    public void mapperFindAllByWriterId() {
        UserEntity userEntity = UserEntity.builder()
                .accountId(randomString())
                .password(randomString())
                .role(UserRole.NORMAL)
                .build();
        // 새로운 유저를 추가한다.
        userMapper.save(userEntity);

        List<Long> savedEntityIds = new ArrayList<>();
        for (int i = 0; i < SIZE; ++i) {
            // 새로 추가한 유저의 이름으로 새로운 글을 작성한다.
            BoardEntity boardEntity = BoardEntity.builder().writer(userEntity).contents("contents").title("title").build();
            boardMapper.save(boardEntity);
            savedEntityIds.add(boardEntity.getId());
            // 저장 후 할당된 id를 savedEntityIds에 추가한다.
        }

        List<BoardEntity> findEntities = boardMapper.findAllByWriterId(userEntity.getId());
        List<Long> findEntityIds = findEntities.stream().map(boardEntity -> boardEntity.getId()).collect(Collectors.toList());

        assertThat(findEntities.size(), equalTo(SIZE));
        assertThat(savedEntityIds, hasItems(findEntityIds.toArray(new Long[SIZE])));
    }

    @Test
    @DisplayName("제목+내용으로 검색한다.")
    public void mapperSearch() {
        UserEntity userEntity = userMapper.findAll().get(0);
        String time = LocalDateTime.now().toString();

        List<Long> savedEntityIds = new ArrayList<>();
        for (int i = 0; i < 20; ++i) {
            BoardEntity boardEntity = null;
            if (i % 2 == 0) {
                boardEntity = BoardEntity.builder().writer(userEntity).contents(time).title("title").build();
            } else {
                boardEntity = BoardEntity.builder().writer(userEntity).contents("contents").title(time).build();
            }
            boardMapper.save(boardEntity);
            savedEntityIds.add(boardEntity.getId());
        }

        List<BoardEntity> findEntities = boardMapper.findAll(SearchType.ALL.name(), time, PAGE, SIZE);
        List<Long> findEntityIds = findEntities.stream().map(boardEntity -> boardEntity.getId()).collect(Collectors.toList());

        assertThat(findEntities.size(), equalTo(SIZE));
        assertThat(savedEntityIds, hasItems(findEntityIds.toArray(new Long[SIZE])));
    }

    @Test
    @DisplayName("TITLE 타입으로 검색할 경우, 해당 키워드를 제목에 포함하고 있는 레코드만 검색해 올 수 있다.")
    public void mapperSearch2() {
        final int INSERT_SIZE = (int) Math.random()*10;
        final String SEARCH_KEYWORD = LocalDateTime.now().toString();

        insertSearchTitleTestData(INSERT_SIZE, SEARCH_KEYWORD);

        List<BoardEntity> findEntities = boardMapper.findAll(SearchType.TITLE.name(), SEARCH_KEYWORD, PAGE, SIZE);

        assertThat(findEntities.size(), equalTo(INSERT_SIZE));
    }

    private void insertSearchTitleTestData(final int INSERT_SIZE, final String SEARCH_KEYWORD) {
        for (int i = 0; i < 20; ++i) {
            BoardEntity boardEntity = null;
            if (i < INSERT_SIZE) {
                boardEntity = BoardEntity.builder().writer(user).contents("contents").title(SEARCH_KEYWORD).build();
            } else {
                boardEntity = BoardEntity.builder().writer(user).contents(SEARCH_KEYWORD).title("title").build();
            }
            boardMapper.save(boardEntity);
        }
    }

    @Test
    @DisplayName("CONTENTS 타입으로 검색할 경우, 해당 키워드를 내용에 포함하고 있는 레코드만 검색해 올 수 있다.")
    public void mapperSearch3() {
        final int INSERT_SIZE = (int) Math.random()*10;
        final String SEARCH_KEYWORD = LocalDateTime.now().toString();

        insertSearchContentsTestData(INSERT_SIZE, SEARCH_KEYWORD);

        List<BoardEntity> findEntities = boardMapper.findAll(SearchType.CONTENTS.name(), SEARCH_KEYWORD, PAGE, SIZE);

        assertThat(findEntities.size(), equalTo(INSERT_SIZE));
    }

    private void insertSearchContentsTestData(final int INSERT_SIZE, final String SEARCH_KEYWORD) {
        for (int i = 0; i < 20; ++i) {
            BoardEntity boardEntity = null;
            if (i < INSERT_SIZE) {
                boardEntity = BoardEntity.builder().writer(user).contents(SEARCH_KEYWORD).title("title").build();
            } else {
                boardEntity = BoardEntity.builder().writer(user).contents("contents").title(SEARCH_KEYWORD).build();
            }
            boardMapper.save(boardEntity);
        }
    }

    @Test
    @DisplayName("검색할 경우 페이징 처리가 잘 되는지 확인한다.")
    public void mapperSearch5() {
        final String SEARCH_KEYWORD = LocalDateTime.now().toString();
        insertTestDataForPaging(SEARCH_KEYWORD);

        List<BoardEntity> findEntities = boardMapper.findAll(SearchType.CONTENTS.name(), SEARCH_KEYWORD, PAGE, SIZE);

        assertThat(findEntities.size(), equalTo(SIZE));
    }

    private void insertTestDataForPaging(final String SEARCH_KEYWORD) {
        final int INSERT_SIZE = 10 + (int)(Math.random()*(50 - 10 + 1));

        for (int i = 0; i < INSERT_SIZE; ++i) {
            boardMapper.save(BoardEntity.builder().writer(user).contents(SEARCH_KEYWORD).title("title").build());
        }
    }

    @Test
    @DisplayName("ALL 타입으로 검색할 경우, count 함수를 이용하여 제목이나 내용에 해당 키워드를 가진 레코드의 총 개수를 가져올 수 있다.")
    public void mapperSearch4() {
        final int INSERT_SIZE = (int) Math.random()*10;;
        final String SEARCH_KEYWORD = LocalDateTime.now().toString();
        insertSearchCountingTestData(INSERT_SIZE, SEARCH_KEYWORD);

        int totalCount = boardMapper.findTotalSizeForSearch(SearchType.ALL.name(), SEARCH_KEYWORD);

        assertThat(totalCount, equalTo(INSERT_SIZE));
    }

    private void insertSearchCountingTestData(final int INSERT_SIZE, final String SEARCH_KEYWORD) {
        List<Long> savedEntityIds = new ArrayList<>();
        for (int i = 0; i < INSERT_SIZE; ++i) {
            BoardEntity boardEntity = null;
            if (i % 2 == 0) {
                boardEntity = BoardEntity.builder().writer(user).contents(SEARCH_KEYWORD).title("title").build();
            } else {
                boardEntity = BoardEntity.builder().writer(user).contents("contents").title(SEARCH_KEYWORD).build();
            }
            boardMapper.save(boardEntity);
            savedEntityIds.add(boardEntity.getId());
        }
    }

    private String randomString() {
        String id = "";
        for (int i = 0; i < 20; i++) {
            double dValue = Math.random();
            if (i % 2 == 0) {
                id += (char) ((dValue * 26) + 65);   // 대문자
                continue;
            }
            id += (char) ((dValue * 26) + 97); // 소문자
        }
        return id;
    }
}
