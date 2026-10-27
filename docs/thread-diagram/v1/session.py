"""v1 확대도: 세션 안쪽. 실행하면 같은 디렉터리에 session.svg를 만든다.

python docs/thread-diagram/v1/session.py
"""

import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent))  # svg.py 위치
from svg import Diagram, LINE

d = Diagram(1000, 510)


def call(points):
    """메서드 호출. TCP와 구분되게 점선으로 그린다."""
    path = "M " + " L ".join(f"{x} {y}" for x, y in points)
    d.out.append(f'<path d="{path}" fill="none" stroke="{LINE["tcp"]}" stroke-width="1.4" '
                 f'stroke-dasharray="5 4" marker-end="url(#arrow-tcp)"/>')


# 제목
d.node(40, 24, 160, 40, "title", ["Client"])
d.node(560, 24, 300, 40, "title", ["Server  :12345"])

# 클라이언트
d.rect(20, 84, 200, 400, "process", r=12)
d.text(120, 272, "Client", weight=600)
d.text(120, 294, "(전체도의 Client A)", size=13, weight=400, color="#57606a")

# 서버
d.rect(340, 84, 640, 400, "process", r=12)

# Session 객체. 스레드가 아니라서 점선 테두리 흰 박스
d.out.append('<rect x="270" y="110" width="440" height="340" rx="10" fill="#ffffff" '
             'stroke="#57606a" stroke-width="1.4" stroke-dasharray="6 4"/>')
d.text(525, 134, "Session", weight=600)
d.text(525, 156, "close()는 CAS로 한 번만 실행", size=12, weight=400, color="#57606a")

# 서버 테두리에 걸친 세션 스레드
d.node(290, 190, 200, 56, "thread", ["Reader", "ReadHandler · readLine()"])
d.node(290, 330, 200, 56, "thread", ["Writer", "WriteHandler · write()"])
d.node(530, 330, 160, 56, "queue", ["sendQueue", "take()로 꺼냄"])
d.text(525, 288, "둘 다 끝나면 SessionHandler가 close() 호출", size=12, weight=400, color="#57606a")
d.line(340, 96, 340, 472)  # 테두리를 박스 위로 다시 그어 "걸친" 모양을 만든다

# 서버 쪽 객체와 스레드
d.node(800, 150, 165, 76, "title", ["SessionManager", "sessions 목록", "add · remove · closeAll"])
d.node(800, 330, 165, 56, "thread", ["main", "콘솔 Scanner"])

# TCP
d.arrow([(220, 218), (288, 218)], "tcp", "TCP", (254, 218))
d.arrow([(290, 358), (222, 358)], "tcp", "TCP", (256, 358))

# 받는 길: Reader -> 로그
d.arrow([(490, 218), (560, 218)], "recv")
d.text(615, 218, "log() 출력", size=13, weight=400, color="#57606a")

# 보내는 길: main -> sendQueue -> Writer
d.arrow([(800, 358), (692, 358)], "send")
d.text(746, 344, "send()", size=11, weight=600, color="#57606a")
d.arrow([(530, 358), (492, 358)], "send")

# Session <-> SessionManager 호출
call([(710, 176), (798, 176)])
d.text(755, 164, "add · remove", size=11, weight=600, color="#57606a")
call([(800, 206), (712, 206)])
d.text(755, 222, "close()", size=11, weight=600, color="#57606a")

d.save(HERE / "session.svg")
