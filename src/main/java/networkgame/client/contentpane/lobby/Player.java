package networkgame.client.contentpane.lobby;

import networkgame.protocol.Direction;

/**
 * 로비에 그릴 플레이어 한 명.
 *
 * @param playerId  서버가 정한 번호. 같은 고양이를 고른 사람끼리도 이 값으로 구분한다
 * @param character 고른 고양이 번호
 * @param x         로비 지도 좌표. 고양이 발밑 가운데 지점이다
 * @param y         로비 지도 좌표. 고양이 발밑 가운데 지점이다
 * @param direction 바라보는 방향. 멈춘 뒤에도 마지막 방향을 유지한다
 * @param walking   {@code true}면 걷기 프레임을 돌려 그린다. 프로토콜에는 없는 값이라 클라이언트가 정한다
 */
record Player(int playerId,
              String name,
              int character,
              double x, double y,
              Direction direction,
              boolean walking) {

    /**
     * 좌표와 방향만 바꾼 플레이어를 돌려준다.
     * <p>걷기 상태는 좌표가 바뀌었는지로 정한다. 같은 좌표를 넘기면 멈춘 그림이 된다.
     */
    Player moveTo(double x, double y, Direction direction) {
        boolean walking = x != this.x || y != this.y;
        return new Player(playerId, name, character, x, y, direction, walking);
    }

    /**
     * 그 자리에서 바라보던 방향 그대로 멈춘 플레이어를 돌려준다.
     * <p>이미 멈춰 있으면 같은 값을 돌려준다.
     */
    Player stop() {
        return moveTo(x, y, direction);
    }
}
