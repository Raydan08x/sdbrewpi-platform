from pathlib import Path
from math import ceil

from reportlab.lib import colors
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "SDBrewPi_E-101_Paquete_Electrico_Preliminar.pdf"
PAGE = landscape(A3)
PW, PH = PAGE

NAVY = colors.HexColor("#10263F")
BLUE = colors.HexColor("#0B69A3")
CYAN = colors.HexColor("#00A6C7")
GREEN = colors.HexColor("#2E7D32")
AMBER = colors.HexColor("#F3A712")
RED = colors.HexColor("#B42318")
LIGHT_RED = colors.HexColor("#FDECEA")
LIGHT_AMBER = colors.HexColor("#FFF4D6")
LIGHT_BLUE = colors.HexColor("#EAF4FA")
LIGHT_GREEN = colors.HexColor("#EAF5EA")
LIGHT_GRAY = colors.HexColor("#F2F4F7")
MID_GRAY = colors.HexColor("#667085")
DARK = colors.HexColor("#1D2939")
WHITE = colors.white


def register_fonts():
    regular = Path(r"C:\Windows\Fonts\arial.ttf")
    bold = Path(r"C:\Windows\Fonts\arialbd.ttf")
    if regular.exists() and bold.exists():
        pdfmetrics.registerFont(TTFont("Tech", str(regular)))
        pdfmetrics.registerFont(TTFont("Tech-Bold", str(bold)))
        return "Tech", "Tech-Bold"
    return "Helvetica", "Helvetica-Bold"


FONT, FONT_BOLD = register_fonts()


def wrap_lines(text, font, size, width):
    lines = []
    for paragraph in str(text).split("\n"):
        if not paragraph:
            lines.append("")
            continue
        words = paragraph.split()
        current = words[0]
        for word in words[1:]:
            candidate = current + " " + word
            if pdfmetrics.stringWidth(candidate, font, size) <= width:
                current = candidate
            else:
                lines.append(current)
                current = word
        lines.append(current)
    return lines


def draw_wrapped(c, text, x, y, width, size=9, leading=None, color=DARK,
                 font=None, max_lines=None):
    font = font or FONT
    leading = leading or size * 1.25
    lines = wrap_lines(text, font, size, width)
    if max_lines and len(lines) > max_lines:
        lines = lines[:max_lines]
        last = lines[-1]
        while pdfmetrics.stringWidth(last + "...", font, size) > width and last:
            last = last[:-1]
        lines[-1] = last.rstrip() + "..."
    c.setFillColor(color)
    c.setFont(font, size)
    for line in lines:
        c.drawString(x, y, line)
        y -= leading
    return y


def box(c, x, y, w, h, title, body="", fill=WHITE, stroke=NAVY,
        title_fill=None, body_size=8.5, title_size=10, line_width=1.2):
    # Allow concise calls that pass body/title sizes positionally after stroke.
    if isinstance(title_fill, (int, float)):
        title_size = body_size
        body_size = title_fill
        title_fill = None
    c.setLineWidth(line_width)
    c.setStrokeColor(stroke)
    c.setFillColor(fill)
    c.roundRect(x, y, w, h, 6, fill=1, stroke=1)
    header_h = 24
    c.setFillColor(title_fill or stroke)
    c.roundRect(x, y + h - header_h, w, header_h, 6, fill=1, stroke=0)
    c.rect(x, y + h - header_h, w, header_h - 6, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont(FONT_BOLD, title_size)
    c.drawCentredString(x + w / 2, y + h - 16, title)
    if body:
        draw_wrapped(c, body, x + 8, y + h - header_h - 14, w - 16,
                     body_size, body_size * 1.22, DARK, FONT)


def page_frame(c, sheet, title, page_no, revision="A"):
    c.setStrokeColor(NAVY)
    c.setLineWidth(1.4)
    c.rect(18, 18, PW - 36, PH - 36, fill=0, stroke=1)
    c.setFillColor(NAVY)
    c.rect(18, PH - 68, PW - 36, 50, fill=1, stroke=0)
    c.setFillColor(WHITE)
    c.setFont(FONT_BOLD, 17)
    c.drawString(34, PH - 48, f"SDBrewPi - {sheet} - {title}")
    c.setFont(FONT, 8.5)
    c.drawRightString(PW - 34, PH - 46, "CONTROL DE FERMENTACION - 2 TANQUES / CHILLER 500 L")

    footer_y = 18
    c.setFillColor(LIGHT_GRAY)
    c.rect(18, footer_y, PW - 36, 42, fill=1, stroke=0)
    c.setStrokeColor(NAVY)
    c.line(18, footer_y + 42, PW - 18, footer_y + 42)
    columns = [18, 430, 720, 925, 1040, PW - 18]
    for xx in columns[1:-1]:
        c.line(xx, footer_y, xx, footer_y + 42)
    c.setFillColor(DARK)
    c.setFont(FONT_BOLD, 7.5)
    c.drawString(26, footer_y + 29, "ESTADO")
    c.setFillColor(RED)
    c.setFont(FONT_BOLD, 10)
    c.drawString(26, footer_y + 12, "PRELIMINAR - NO CONSTRUIR / NO ENERGIZAR")
    c.setFillColor(DARK)
    c.setFont(FONT_BOLD, 7.5)
    c.drawString(440, footer_y + 29, "BASE")
    c.setFont(FONT, 8.5)
    c.drawString(440, footer_y + 12, "Fotos + firmware LVGL + fichas oficiales")
    c.setFont(FONT_BOLD, 7.5)
    c.drawString(730, footer_y + 29, "REVISION")
    c.setFont(FONT, 9)
    c.drawString(730, footer_y + 12, revision)
    c.setFont(FONT_BOLD, 7.5)
    c.drawString(935, footer_y + 29, "HOJA")
    c.setFont(FONT, 9)
    c.drawString(935, footer_y + 12, f"{page_no}/9")
    c.setFont(FONT_BOLD, 7.5)
    c.drawString(1050, footer_y + 29, "FECHA")
    c.setFont(FONT, 9)
    c.drawString(1050, footer_y + 12, "2026-09-13")


def arrow(c, x1, y1, x2, y2, color=NAVY, width=2, label=None, label_y=8):
    c.setStrokeColor(color)
    c.setFillColor(color)
    c.setLineWidth(width)
    c.line(x1, y1, x2, y2)
    angle_horizontal = abs(x2 - x1) >= abs(y2 - y1)
    if angle_horizontal:
        direction = 1 if x2 >= x1 else -1
        c.line(x2, y2, x2 - 8 * direction, y2 + 4)
        c.line(x2, y2, x2 - 8 * direction, y2 - 4)
    else:
        direction = 1 if y2 >= y1 else -1
        c.line(x2, y2, x2 + 4, y2 - 8 * direction)
        c.line(x2, y2, x2 - 4, y2 - 8 * direction)
    if label:
        c.setFont(FONT_BOLD, 7.5)
        c.drawCentredString((x1 + x2) / 2, (y1 + y2) / 2 + label_y, label)


def pill(c, x, y, text, fill, width=None):
    width = width or pdfmetrics.stringWidth(text, FONT_BOLD, 8) + 18
    c.setFillColor(fill)
    c.roundRect(x, y, width, 18, 9, fill=1, stroke=0)
    c.setFillColor(WHITE if fill != LIGHT_AMBER else DARK)
    c.setFont(FONT_BOLD, 8)
    c.drawCentredString(x + width / 2, y + 5, text)
    return width


def draw_table(c, x, top, widths, headers, rows, font_size=7.5,
               header_fill=NAVY, row_fills=(WHITE, LIGHT_GRAY), row_height=27):
    total = sum(widths)
    c.setFillColor(header_fill)
    c.rect(x, top - row_height, total, row_height, fill=1, stroke=0)
    cx = x
    c.setFont(FONT_BOLD, font_size)
    c.setFillColor(WHITE)
    for i, head in enumerate(headers):
        draw_wrapped(c, head, cx + 5, top - 10, widths[i] - 10,
                     font_size, font_size * 1.05, WHITE, FONT_BOLD, 2)
        cx += widths[i]
    y = top - row_height
    for ridx, row in enumerate(rows):
        fill = row_fills[ridx % len(row_fills)]
        c.setFillColor(fill)
        c.rect(x, y - row_height, total, row_height, fill=1, stroke=0)
        cx = x
        for i, value in enumerate(row):
            color = RED if isinstance(value, str) and value.startswith("HOLD:") else DARK
            draw_wrapped(c, value, cx + 5, y - 10, widths[i] - 10,
                         font_size, font_size * 1.08, color,
                         FONT_BOLD if i == 0 else FONT, 3)
            cx += widths[i]
        y -= row_height
    c.setStrokeColor(colors.HexColor("#98A2B3"))
    c.setLineWidth(0.45)
    for i in range(len(rows) + 2):
        yy = top - i * row_height
        c.line(x, yy, x + total, yy)
    cx = x
    c.line(cx, top, cx, y)
    for w in widths:
        cx += w
        c.line(cx, top, cx, y)
    return y


def cover(c):
    page_frame(c, "E-100", "PORTADA Y BASE DE DISENO", 1)
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 31)
    c.drawString(54, PH - 130, "PAQUETE TECNICO ELECTRICO")
    c.setFillColor(BLUE)
    c.setFont(FONT_BOLD, 20)
    c.drawString(54, PH - 165, "Control de chiller, bomba y fermentadores F1/F2")
    pill(c, 54, PH - 208, "REVISION A", BLUE, 92)
    pill(c, 156, PH - 208, "VALIDACION REQUERIDA", AMBER, 150)
    pill(c, 316, PH - 208, "SIN AUTORIZACION DE ENERGIZACION", RED, 220)

    box(c, 54, 390, 510, 190, "ALCANCE CONFIRMADO",
        "HMI Waveshare ESP32-S3-Touch-LCD-4.3B.\n"
        "Modulo Waveshare ESP32-S3-Relay-6CH.\n"
        "Variador Power PD2000 3 32: entrada 1PH 220-240 VAC, 23 A; salida 3PH 0-240 VAC, 9.6 A.\n"
        "Bomba Pedrollo PKm 60: 110 VAC, 60 Hz, 0.37 kW, 5.5 A.\n"
        "Transformador elevador PEC rotulado 3 kVA, 110/220 V.\n"
        "Chiller: deposito 500 L y compresor reportado de 3 HP, 220 V trifasico.",
        LIGHT_BLUE, BLUE, body_size=11)

    box(c, 590, 390, 545, 190, "CORRECCIONES AL DOCUMENTO BASE",
        "1. GPIO4 de la HMI ya es TP_IRQ del tactil: no conectar DS18B20 alli.\n"
        "2. El firmware actual simula temperaturas; no implementa lectura real de Pills, repetidores ni DS18B20.\n"
        "3. La corriente y capacidad del transformador deben verificarse antes de usar el VFD.\n"
        "4. Los parametros P0/P4/P8 y el limite de 40/42 Hz quedan retirados hasta obtener manual del PD2000 y aprobacion del fabricante del compresor.\n"
        "5. Los interlocks frigorificos existentes no se puentean ni se reemplazan por software.",
        LIGHT_AMBER, AMBER, body_size=10.4)

    c.setFillColor(DARK)
    c.setFont(FONT_BOLD, 13)
    c.drawString(54, 350, "Uso previsto")
    draw_wrapped(c,
        "Documento para que el ingeniero o tecnico electricista levante datos faltantes, seleccione protecciones, complete planos de taller y ejecute pruebas controladas. Toda seleccion final debe cumplir RETIE, NTC 2050, instrucciones de los fabricantes y el analisis de riesgo de la planta.",
        54, 326, 1080, 11, 15, DARK, FONT)

    box(c, 54, 120, 1080, 150, "PUNTOS DE BLOQUEO ANTES DE OBRA",
        "H1 - Fotografia legible de la placa completa del compresor y diagrama de bornes.    "
        "H2 - Manual exacto del VFD Power PD2000 3 32.    "
        "H3 - Placa completa y tipo (aislamiento/autotransformador) del PEC 3 kVA.\n"
        "H4 - Tension y potencia reales de las electrovavulas.    "
        "H5 - Tension disponible, sistema de puesta a tierra y corriente de cortocircuito.    "
        "H6 - Longitudes, canalizaciones, temperatura y agrupamiento de conductores.\n"
        "H7 - Inventario y esquema funcional de presostatos, termicos, flujo, nivel y proteccion de aceite ya instalados en el chiller.",
        LIGHT_RED, RED, body_size=10.5)


def power_diagram(c):
    page_frame(c, "E-101", "DIAGRAMA UNIFILAR DE FUERZA", 2)
    y0 = 665
    box(c, 40, y0, 120, 70, "RED 110 VAC", "L / N / PE\nDatos de red: HOLD", LIGHT_BLUE, BLUE, 8.5)
    box(c, 200, y0, 135, 70, "DS1", "Seccionador general\n2P bloqueable - TBD", WHITE, NAVY, 8.5)
    arrow(c, 160, y0 + 35, 200, y0 + 35, BLUE, 3, "L,N,PE")

    branch_x = 365
    c.setStrokeColor(NAVY)
    c.setLineWidth(3)
    c.line(335, y0 + 35, branch_x, y0 + 35)
    c.line(branch_x, 190, branch_x, y0 + 35)
    c.setFont(FONT_BOLD, 8)
    c.setFillColor(NAVY)
    c.drawString(branch_x + 6, y0 + 42, "BARRAS L / N / PE")

    rows = [610, 490, 370, 250]
    labels = ["RAMA COMPRESOR", "RAMA BOMBA", "RAMA CONTROL 24 VDC", "RESERVA / SERVICIOS"]
    for y, label in zip(rows, labels):
        c.setStrokeColor(NAVY)
        c.setLineWidth(2)
        c.line(branch_x, y + 35, 405, y + 35)
        c.setFillColor(MID_GRAY)
        c.setFont(FONT_BOLD, 7.5)
        c.drawString(405, y + 75, label)

    # Compressor branch
    box(c, 405, rows[0], 105, 70, "QF1", "Proteccion T1\n2P - calcular", WHITE, AMBER, 8.5)
    box(c, 550, rows[0], 150, 70, "T1 PEC 3 kVA", "110/220 V elevador\nTIPO Y CAPACIDAD: HOLD", LIGHT_AMBER, AMBER, 8.2)
    box(c, 740, rows[0], 105, 70, "QF2", "Proteccion VFD\n2P - segun manual", WHITE, AMBER, 8.2)
    box(c, 885, rows[0], 125, 70, "V1 PD2000", "IN 1PH 220-240 V\nOUT 3PH 0-240 V", LIGHT_BLUE, BLUE, 8.2)
    box(c, 1050, rows[0], 100, 70, "M1", "Compresor 3 HP\nplaca: HOLD", LIGHT_RED, RED, 8.2)
    for x1, x2, lab in [(510,550,"110 VAC"),(700,740,"220 VAC"),(845,885,"220 VAC"),(1010,1050,"U/V/W + PE")]:
        arrow(c, x1, rows[0]+35, x2, rows[0]+35, BLUE if "VAC" in lab else RED, 2.5, lab)

    # Pump branch
    box(c, 405, rows[1], 105, 70, "QF3", "Proteccion bomba\n2P - calcular", WHITE, AMBER, 8.5)
    box(c, 565, rows[1], 130, 70, "KMP", "Contactor 2P AC-3\nBobina 24 VDC", LIGHT_BLUE, BLUE, 8.5)
    box(c, 750, rows[1], 125, 70, "OLP", "Sobrecarga motor\nRango incluye 5.5 A", LIGHT_AMBER, AMBER, 8.2)
    box(c, 930, rows[1], 180, 70, "M2 PEDROLLO PKm 60", "110 VAC / 60 Hz / 5.5 A\n0.37 kW P2 - IPX4", LIGHT_GREEN, GREEN, 8.5)
    for x1, x2, lab in [(510,565,"L,N,PE"),(695,750,"L,N"),(875,930,"L,N,PE")]:
        arrow(c, x1, rows[1]+35, x2, rows[1]+35, GREEN, 2.5, lab)

    # Control branch
    box(c, 405, rows[2], 105, 70, "QF4", "Proteccion fuente\n2P - calcular", WHITE, AMBER, 8.3)
    box(c, 560, rows[2], 145, 70, "PS1 24 VDC", "Fuente industrial\nCorriente: TBD + 30%", LIGHT_BLUE, BLUE, 8.4)
    box(c, 755, rows[2], 112, 70, "FU-PLC", "Rama HMI\n24 VDC / TBD", WHITE, CYAN, 8.5)
    box(c, 902, rows[2], 112, 70, "FU-RLY", "Rama Relay 6CH\n24 VDC / TBD", WHITE, CYAN, 8.2)
    box(c, 1048, rows[2], 102, 70, "FU-YV", "Valvulas 24 VDC\nSOLO si se confirma", LIGHT_AMBER, AMBER, 8.0)
    for x1,x2,lab in [(510,560,"110 VAC"),(705,755,"24 VDC"),(867,902,"24 VDC"),(1014,1048,"24 VDC")]:
        arrow(c,x1,rows[2]+35,x2,rows[2]+35,CYAN,2.2,lab)

    # Reserve
    box(c, 405, rows[3], 105, 70, "QF5", "Reserva / tomas\nDefinir en campo", WHITE, MID_GRAY, 8.3)
    box(c, 565, rows[3], 215, 70, "SERVICIOS AUXILIARES", "Ventilacion, luz de tablero o fuente separada.\nNo compartir sin calculo de cargas.", LIGHT_GRAY, MID_GRAY, 8.3)
    arrow(c, 510, rows[3]+35, 565, rows[3]+35, MID_GRAY, 2.2, "TBD")

    c.setFillColor(LIGHT_RED)
    c.roundRect(405, 85, 745, 75, 6, fill=1, stroke=0)
    c.setFillColor(RED)
    c.setFont(FONT_BOLD, 10)
    c.drawString(417, 139, "HOLD H2/H3: TRANSFORMADOR Y PROTECCION DEL VFD")
    draw_wrapped(c,
        "T1 rotulado 3 kVA entrega idealmente 13.6 A a 220 V. La placa de V1 declara 23 A de entrada a 220-240 V. QF2=20 A del documento original queda rechazado hasta obtener manual, corriente real de aplicacion y calculo de alimentacion. PE es continuo: no se conmuta ni se protege con fusible.",
        417, 120, 720, 9.2, 12, RED, FONT)


def control_diagram(c):
    page_frame(c, "E-102", "MANDO, INTERLOCKS Y SALIDAS", 3)
    c.setFillColor(LIGHT_BLUE)
    c.rect(35, 655, 1120, 70, fill=1, stroke=0)
    c.setFillColor(DARK)
    c.setFont(FONT_BOLD, 11)
    c.drawString(48, 705, "PRINCIPIO DE CONTROL")
    draw_wrapped(c,
        "La placa Waveshare entrega contactos secos. CH1 ordena RUN al VFD; CH2 gobierna la bobina del contactor de la bomba; CH3 y CH4 alimentan las valvulas. Los presostatos, termicos, proteccion de aceite, flujo y paro de emergencia forman una cadena cableada y conservan autoridad aunque falle el ESP32.",
        48, 684, 1090, 9.5, 12, DARK, FONT)

    # Ladder rails
    x_left, x_right = 55, 1135
    c.setStrokeColor(RED)
    c.setLineWidth(3)
    c.line(x_left, 165, x_left, 500)
    c.setStrokeColor(BLUE)
    c.line(x_right, 165, x_right, 500)
    c.setFillColor(RED); c.setFont(FONT_BOLD, 9); c.drawString(40, 508, "+24 VDC PS1")
    c.setFillColor(BLUE); c.drawRightString(1150, 508, "0 VDC PS1")

    rung_y = [565, 455, 345, 235]
    rung_names = ["R1 - ORDEN VFD", "R2 - BOMBA", "R3 - VALVULA F1", "R4 - VALVULA F2"]
    for index, (yy,name) in enumerate(zip(rung_y,rung_names)):
        c.setFillColor(MID_GRAY); c.setFont(FONT_BOLD,8)
        c.drawString(72, yy + (57 if index == 0 else 42), name)
        if index > 0:
            c.setStrokeColor(DARK); c.setLineWidth(1.8); c.line(x_left,yy,x_right,yy)

    # R1 VFD: isolated from PS1. All elements are dry contacts in the VFD logic loop.
    c.setFillColor(LIGHT_AMBER)
    c.roundRect(55, 518, 1080, 94, 6, fill=1, stroke=0)
    c.setFillColor(RED)
    c.setFont(FONT_BOLD, 8)
    c.drawString(64, 603, "LAZO LOGICO INTERNO DEL VFD - NO INYECTAR 24 VDC DESDE PS1")
    items1 = [
        (65, "VFD COM/P24\nHOLD MANUAL", RED),
        (205, "S0 E-STOP\nNC", RED),
        (345, "PSH / PSL\nPRESION NC", AMBER),
        (485, "TS / OIL\nTERMICO NC", AMBER),
        (625, "FS1 FLUJO\nNC", AMBER),
        (765, "CH1 RELAY\nCOM-NO", BLUE),
        (905, "VFD DI1\nRUN/ENABLE", GREEN),
    ]
    for xx,title,stroke in items1:
        box(c, xx, 535, 105, 60, title, "", WHITE, stroke, title_size=8.0)
    for a,b in zip(items1[:-1],items1[1:]):
        arrow(c,a[0]+105,565,b[0],565,DARK,1.8)
    c.setFillColor(RED); c.setFont(FONT_BOLD,7.5)
    c.drawString(1020, 568, "Bornes y logica")
    c.drawString(1020, 556, "HOLD por manual")

    # R2 pump
    box(c, 115, 425, 120, 60, "S0 E-STOP NC", "", WHITE, RED, title_size=8.5)
    box(c, 300, 425, 120, 60, "OLP 95-96 NC", "", WHITE, AMBER, title_size=8.5)
    box(c, 485, 425, 120, 60, "CH2 COM-NO", "", WHITE, BLUE, title_size=8.5)
    box(c, 705, 425, 145, 60, "KMP A1 / A2", "Bobina 24 VDC", LIGHT_GREEN, GREEN, 8.2, 8.5)
    for x1,x2 in [(235,300),(420,485),(605,705),(850,1135)]: arrow(c,x1,455,x2,455,DARK,1.8)

    # R3/R4 valves
    for yy,ch,tag in [(345,"CH3 COM-NO","YV-F1 24 VDC"),(235,"CH4 COM-NO","YV-F2 24 VDC")]:
        box(c, 180, yy-30, 140, 60, ch, "Rama DC fusionada", WHITE, BLUE, 8.0, 8.5)
        box(c, 520, yy-30, 155, 60, tag, "Bobina + supresor", LIGHT_GREEN, GREEN, 8.0, 8.5)
        arrow(c,55,yy,180,yy,DARK,1.8)
        arrow(c,320,yy,520,yy,DARK,1.8,"18 AWG / TBD")
        arrow(c,675,yy,1135,yy,DARK,1.8)
        c.setStrokeColor(CYAN); c.circle(710,yy,10,fill=0,stroke=1)
        c.setFont(FONT_BOLD,7); c.setFillColor(CYAN); c.drawString(727,yy+12,"Diodo/TVS segun polaridad")

    box(c, 55, 80, 530, 90, "MAPA DE CANALES CONFIRMADO POR FIRMWARE",
        "CH1 GPIO1: VFD RUN.  CH2 GPIO2: bomba KMP.  CH3 GPIO41: valvula F1.  CH4 GPIO42: valvula F2.  CH5 GPIO45 y CH6 GPIO46: RESERVA. Buzzer GPIO21. Contactos nominales del modulo: hasta 10 A a 250 VAC o 30 VDC, pero las cargas inductivas deben usar contactor/supresion y margen.",
        LIGHT_BLUE, BLUE, body_size=9.2)
    box(c, 610, 80, 525, 90, "REGLAS DE CABLEADO",
        "No aplicar 24 V externos a las entradas digitales del VFD sin conocer su logica. No instalar contactor entre U/V/W y el compresor durante marcha. Separar potencia VFD de control y comunicaciones. Unir la pantalla del cable del motor con prensa EMC y PE segun manual. Mantener todas las protecciones frigorificas existentes.",
        LIGHT_RED, RED, body_size=9.2)


def instrumentation(c):
    page_frame(c, "E-103", "INSTRUMENTACION Y COMUNICACIONES", 4)
    c.setFillColor(LIGHT_AMBER)
    c.roundRect(35, 650, 1120, 72, 6, fill=1, stroke=0)
    c.setFillColor(RED)
    c.setFont(FONT_BOLD, 11)
    c.drawString(48, 698, "CORRECCION OBLIGATORIA")
    draw_wrapped(c,
        "GPIO4 de la ESP32-S3-Touch-LCD-4.3B es TP_IRQ del controlador tactil y el firmware lo usa durante el arranque. La instruccion del documento original de conectar un DS18B20 a GPIO4 es incompatible. No cablear una sonda en ese pin.",
        48, 677, 1090, 9.8, 13, RED, FONT_BOLD)

    # Architecture blocks
    box(c, 45, 465, 205, 120, "FERMENTADORES F1/F2",
        "Pills BLE\nTemperatura + gravedad\nBateria pendiente de carga",
        LIGHT_GREEN, GREEN, body_size=10)
    box(c, 310, 465, 205, 120, "REPETIDORES",
        "ESP32-C3 BLE -> WiFi\nFirmware y contrato MQTT\npendientes de prueba real",
        LIGHT_BLUE, CYAN, body_size=10)
    box(c, 575, 465, 210, 120, "RASPBERRY PI 5",
        "MQTT + Spring Boot\nPostgreSQL / WebApp\nSupervision y trazabilidad",
        LIGHT_BLUE, BLUE, body_size=10)
    box(c, 845, 465, 260, 120, "HMI / CONTROLADOR LOCAL",
        "Waveshare 4.3B\nLVGL + logica local\nFirmware actual: datos simulados",
        LIGHT_AMBER, AMBER, body_size=10)
    arrow(c,250,525,310,525,GREEN,2.5,"BLE")
    arrow(c,515,525,575,525,CYAN,2.5,"MQTT/WiFi")
    arrow(c,785,525,845,525,BLUE,2.5,"HTTP/MQTT TBD")

    box(c, 845, 285, 260, 100, "RELAY 6CH - TABLERO",
        "ESP-NOW cada 400 ms\nWatchdog firmware: 15 s\nContactos CH1..CH6",
        LIGHT_BLUE, BLUE, body_size=10)
    arrow(c,975,465,975,385,BLUE,3,"ESP-NOW")

    box(c, 45, 285, 250, 100, "TT-CH FUTURO",
        "Sonda del deposito del chiller.\nPreferencia: PT100 clase A + transmisor RS485 Modbus aislado, o modulo analogico industrial.",
        LIGHT_GRAY, MID_GRAY, body_size=9.5)
    box(c, 365, 285, 245, 100, "BUS RS485 AISLADO",
        "A/B + referencia segun fabricante.\nPar trenzado apantallado.\n120 ohm solo en extremos.",
        LIGHT_BLUE, CYAN, body_size=9.5)
    box(c, 675, 285, 110, 100, "HMI RS485", "GPIO43 RX\nGPIO44 TX\nBornes A/B", LIGHT_BLUE, CYAN, body_size=9)
    arrow(c,295,335,365,335,CYAN,2.3,"MODBUS")
    arrow(c,610,335,675,335,CYAN,2.3,"A/B")
    arrow(c,785,335,845,335,MID_GRAY,1.6,"INTERNO")

    box(c, 45, 105, 330, 105, "ALIMENTACION HMI",
        "Opcion recomendada para instalacion fija: 24 VDC protegido hacia la entrada 7-36 VDC de la HMI. USB-C queda para servicio/programacion. No alimentar simultaneamente por ambos medios sin confirmacion del fabricante.",
        LIGHT_GREEN, GREEN, body_size=9.4)
    box(c, 420, 105, 330, 105, "SEGREGACION",
        "Separar fisicamente VFD/motor, 110/220 VAC, 24 VDC de bobinas y comunicaciones. Cruzar potencia y senal a 90 grados. La antena ESP32 debe quedar fuera de pantallas metalicas o con separacion adecuada.",
        LIGHT_BLUE, BLUE, body_size=9.4)
    box(c, 795, 105, 310, 105, "AUTORIDAD DE CONTROL",
        "El controlador local conserva interlocks y salida segura. Spring Boot gestiona receta, lote, historico y comandos auditados. La perdida del servidor no debe dejar cargas activas ni anular protecciones cableadas.",
        LIGHT_AMBER, AMBER, body_size=9.4)


def terminals(c):
    page_frame(c, "E-104", "LISTA DE CONEXIONES E I/O", 5)
    headers = ["ID", "ORIGEN", "DESTINO", "SENAL / NIVEL", "CONDUCTOR PRELIMINAR", "CONDICION"]
    rows = [
        ["PWR-01", "Red 110 VAC", "DS1", "L/N/PE", "TBD por calculo", "HOLD: red y cortocircuito"],
        ["PWR-02", "DS1/QF1", "T1 PEC", "110 VAC", "TBD por ampacidad", "HOLD: placa completa T1"],
        ["PWR-03", "T1/QF2", "V1 PD2000", "220 VAC 1PH", "TBD por manual VFD", "HOLD: 3 kVA vs VFD 23 A"],
        ["PWR-04", "V1 U/V/W", "M1 compresor", "0-240 VAC 3PH", "Cable VFD apantallado + PE", "HOLD: placa y conexion M1"],
        ["PWR-05", "DS1/QF3", "KMP/OLP/M2", "110 VAC 1PH", "TBD; referencia 5.5 A", "Verificar caida y canalizacion"],
        ["DC-01", "PS1/FU-PLC", "HMI 4.3B", "24 VDC", "2x18 AWG o calculado", "Entrada oficial 7-36 VDC"],
        ["DC-02", "PS1/FU-RLY", "Relay 6CH", "24 VDC", "2x18 AWG o calculado", "Entrada oficial 7-36 VDC"],
        ["CMD-01", "Relay CH1 COM/NO", "VFD DCM/DI1", "Contacto seco", "Par 18-20 AWG", "HOLD: bornes/manual VFD"],
        ["CMD-02", "Relay CH2 COM/NO", "KMP A1", "24 VDC", "18 AWG", "OLP 95-96 + E-stop en serie"],
        ["CMD-03", "Relay CH3 COM/NO", "YV-F1", "24 VDC previsto", "18 AWG", "HOLD: placa bobina + supresor"],
        ["CMD-04", "Relay CH4 COM/NO", "YV-F2", "24 VDC previsto", "18 AWG", "HOLD: placa bobina + supresor"],
        ["CMD-05", "Relay CH5", "Reserva", "Contacto seco", "Sin cablear", "ECO prohibido sin aprobacion"],
        ["CMD-06", "Relay CH6", "Reserva", "Contacto seco", "Sin cablear", "Definir futuro"],
        ["FB-01", "VFD TA/TB/TC", "DI aislada", "Fallo VFD", "Par 18-20 AWG", "HOLD: logica del contacto"],
        ["FB-02", "KMP 13/14", "DI aislada", "Bomba confirmada", "Par 18-20 AWG", "Opcional recomendado"],
        ["NET-01", "HMI", "Relay 6CH", "ESP-NOW 2.4 GHz", "Sin conductor", "Enlace local + watchdog"],
        ["NET-02", "Repetidores", "Raspberry Pi 5", "MQTT sobre WiFi", "Sin conductor", "Pendiente prueba real"],
        ["INST-01", "TT-CH RS485", "HMI A/B", "Modbus RTU", "Par trenzado apantallado", "Sensor por adquirir"],
    ]
    draw_table(c, 35, 720, [62,150,155,140,190,423], headers, rows, 7.2, row_height=31)
    c.setFillColor(RED)
    c.setFont(FONT_BOLD, 9)
    c.drawString(40, 96, "NOTA: los calibres mostrados como referencia no autorizan compra ni instalacion hasta completar H1-H7.")


def bom(c):
    page_frame(c, "E-105", "LISTADO PRELIMINAR DE MATERIALES", 6)
    headers = ["ITEM", "TAG", "CANT.", "DESCRIPCION TECNICA", "ESTADO / CRITERIO DE SELECCION"]
    rows = [
        ["1", "HMI1", "1", "Waveshare ESP32-S3-Touch-LCD-4.3B, 800x480", "EXISTENTE - 5 V USB o 7-36 VDC"],
        ["2", "RLY1", "1", "Waveshare ESP32-S3-Relay-6CH, 1NO+1NC", "EXISTENTE - 7-36 VDC; contacto <=10 A"],
        ["3", "V1", "1", "Power PD2000 3 32, 3 HP / 220 V", "EXISTENTE - HOLD manual exacto"],
        ["4", "T1", "1", "PEC elevador 3 kVA, 110/220 V", "EXISTENTE - HOLD placa, tipo y capacidad"],
        ["5", "M2", "1", "Pedrollo PKm 60, 110 V, 5.5 A, 0.37 kW", "EXISTENTE - bomba independiente del VFD"],
        ["6", "ENC1", "1", "Gabinete industrial con placa, riel DIN y canaleta", "COMPRAR - IP/IK y tamano por disipacion/ambiente"],
        ["7", "DS1", "1", "Seccionador principal 2P bloqueable", "COMPRAR - corriente, SCCR y categoria TBD"],
        ["8", "QF1..QF4", "4", "Protecciones bipolares de ramas", "COMPRAR - segun calculo y manuales; no fijar 20 A"],
        ["9", "SPD/RCD", "TBD", "Proteccion sobretension y diferencial", "HOLD: red, RETIE y compatibilidad VFD"],
        ["10", "PS1", "1", "Fuente industrial 110 VAC / 24 VDC", "COMPRAR - suma de cargas + 30%, certificada"],
        ["11", "KMP", "1", "Contactor 2P AC-3 >=9 A, bobina 24 VDC", "COMPRAR - para bomba 5.5 A"],
        ["12", "OLP", "1", "Rele termico cuyo rango incluya 5.5 A", "COMPRAR - ajustar a placa y coordinar con KMP"],
        ["13", "S0", "1", "Paro de emergencia con contactos NC", "HOLD: nivel de seguridad segun riesgo"],
        ["14", "FU-DC", "4-6", "Portafusibles DC y fusibles por carga", "COMPRAR - HMI, relay, F1, F2 y reservas"],
        ["15", "YV1/YV2", "2", "Electrovalvulas para camisas", "HOLD: tension, potencia, NC/NO y presion"],
        ["16", "SUP", "3+", "Supresores para bobinas DC/AC", "COMPRAR - diodo/TVS/RC segun cada bobina"],
        ["17", "TT-CH", "1", "PT100 clase A + transmisor RS485 Modbus aislado", "RECOMENDADO - sensor del deposito pendiente"],
        ["18", "TB", "TBD", "Bornes L/N/PE/220/24/0V, topes y cubiertas", "COMPRAR - separar tensiones y reservar 20%"],
        ["19", "CAB-PWR", "TBD", "Cable de fuerza cobre, 600 V, terminales/ferrules", "HOLD: ampacidad, caida, ambiente y longitud"],
        ["20", "CAB-VFD", "TBD", "Cable VFD apantallado U/V/W/PE + prensa EMC", "HOLD: manual, longitud y corriente M1"],
        ["21", "CAB-CTL", "TBD", "Par trenzado y cable flexible 18-20 AWG", "COMPRAR - por recorrido; identificar ambos extremos"],
        ["22", "PE", "TBD", "Barra PE, puentes, trenza de puerta y herrajes", "COMPRAR - continuidad equipotencial obligatoria"],
        ["23", "LBL", "1 lote", "Marcadores, etiquetas, senalizacion y portaplano", "COMPRAR - conforme planos finales"],
    ]
    draw_table(c, 35, 720, [45,70,55,430,520], headers, rows, 7.1, row_height=26)


def holds(c):
    page_frame(c, "E-106", "VERIFICACIONES DE INGENIERIA", 7)
    box(c, 40, 550, 530, 170, "HOLD CRITICO - ALIMENTACION VFD",
        "Dato visible T1: 3 kVA, 110/220 V. Corriente secundaria ideal: 3000 VA / 220 V = 13.6 A.\n\n"
        "Dato visible V1: entrada 1PH 220-240 V, 23 A; salida 3PH 0-240 V, 9.6 A.\n\n"
        "Resultado: el transformador no se aprueba con la informacion disponible. Confirmar regimen real, factor de servicio, eficiencia, armonicos, capacidad continua, caida de tension y recomendacion del fabricante del VFD.",
        LIGHT_RED, RED, body_size=10.2)
    box(c, 610, 550, 540, 170, "HOLD CRITICO - COMPRESOR Y VFD",
        "Se requiere placa del compresor: tension, corriente, conexion delta/estrella, refrigerante, proteccion termica y regimen.\n\n"
        "No programar 40 Hz, 42 Hz, P0-02, P4-00 ni ningun codigo del documento base sin el manual exacto. La velocidad minima depende del compresor y de lubricacion, retorno de aceite, presiones y enfriamiento del motor.\n\n"
        "No efectuar prueba en vacio del VFD con el compresor conectado sin protocolo frigorifico.",
        LIGHT_RED, RED, body_size=10.2)

    left = [
        "Levantamiento de red: 110 V nominal, fases, neutro, PE, frecuencia, capacidad del circuito y cortocircuito disponible.",
        "Identificar si T1 es autotransformador o transformador de aislamiento; definir proteccion en primario/secundario y conexion de PE.",
        "Calcular conductores con ampacidad, temperatura, agrupamiento, caida de tension, longitud, terminales y proteccion mecanica.",
        "Revisar compatibilidad electromagnetica del VFD, puesta a tierra, pantalla 360 grados y separacion de control.",
    ]
    right = [
        "Inventariar presostato alto/bajo, termico, proteccion de aceite, control de flujo, nivel minimo y contacto de falla VFD.",
        "Confirmar tension/potencia y estado normal de YV1/YV2; seleccionar fusibles y supresores por bobina.",
        "Definir parada de emergencia y categoria de seguridad mediante evaluacion de riesgo; el ESP32 no es rele de seguridad.",
        "Actualizar planos como-construido, ajustes, torques, pruebas y firmas antes de entregar a operacion.",
    ]
    box(c, 40, 230, 530, 270, "CALCULOS Y DATOS QUE COMPLETA EL ELECTRICISTA", "", WHITE, NAVY)
    y=465
    for i,t in enumerate(left,1):
        pill(c,58,y-4,str(i),BLUE,20)
        y=draw_wrapped(c,t,88,y,455,9.5,12.5,DARK,FONT)-16
    box(c, 610, 230, 540, 270, "INTERLOCKS Y VALIDACION FUNCIONAL", "", WHITE, NAVY)
    y=465
    for i,t in enumerate(right,5):
        pill(c,628,y-4,str(i),GREEN,20)
        y=draw_wrapped(c,t,658,y,465,9.5,12.5,DARK,FONT)-16

    box(c, 40, 90, 1110, 100, "CRITERIO DE LIBERACION",
        "Solo puede cambiarse el sello del plano a 'APROBADO PARA CONSTRUCCION' cuando un profesional competente complete los HOLD, firme el calculo, adjunte manuales y placas, verifique RETIE/NTC 2050 y emita planos de taller con referencias exactas. El presente paquete sirve para levantar y revisar; no autoriza conexion de potencia.",
        LIGHT_AMBER, AMBER, body_size=11)


def commissioning(c):
    page_frame(c, "E-107", "PROTOCOLO DE PRUEBAS Y ENTREGA", 8)
    headers = ["PASO", "CONDICION", "PRUEBA", "CRITERIO DE ACEPTACION", "REGISTRO"]
    rows = [
        ["1", "LOTO aplicado", "Inspeccion de materiales, tags, separacion y aprietes", "Coincide con planos aprobados y torques del fabricante", "Firma + fotos"],
        ["2", "Sin tension", "Continuidad PE de gabinete, puerta, VFD, motor, bomba y chiller", "Valor y metodo aceptados por profesional responsable", "Ohmios / instrumento"],
        ["3", "Sin electronica conectada", "Aislamiento de circuitos de potencia", "Segun equipo, norma y fabricante; proteger VFD/ESP32", "MOhm / tension ensayo"],
        ["4", "Solo QF4", "Medir PS1 y polaridad en cada fusible DC", "24 VDC dentro de tolerancia, sin inversion", "VDC por punto"],
        ["5", "Banco baja tension", "Energizar HMI y Relay 6CH sin cargas de potencia", "Arranque normal, sin calentamiento, salidas desenergizadas", "Hora / corriente"],
        ["6", "Cargas simuladas", "Probar CH1-CH4 con lamparas o simuladores", "Asignacion coincide; CH5/CH6 permanecen reserva", "Checklist I/O"],
        ["7", "Enlace activo", "Interrumpir alimentacion/comunicacion HMI", "Relay abre salidas por watchdog; verificar tiempo y rearme seguro", "Tiempo medido"],
        ["8", "Interlocks cableados", "Abrir uno a uno E-stop, presostatos, termico, aceite, flujo y OLP", "Cada apertura inhibe la carga correspondiente sin software", "Matriz causa-efecto"],
        ["9", "Motor desacoplado si aplica", "Prueba del arrancador de bomba y sentido de flujo", "OLP ajustado; corriente <= placa; sin cavitacion", "A / bar / caudal"],
        ["10", "Manual VFD aprobado", "Verificar parametrizacion sin arrancar compresor", "Datos de placa, rampas y limites firmados", "Backup parametros"],
        ["11", "Tecnico frigorista presente", "Arranque controlado del compresor", "Corrientes, presiones, aceite y temperaturas dentro de limites", "Formato frigorifico"],
        ["12", "Sistema completo", "Prueba AUTO con agua/glicol y setpoint conservador", "Estabilidad, anti-ciclo, flujo y alarmas verificadas", "Tendencia + firma"],
        ["13", "Falla simulada", "Perdida WiFi, servidor y sensor", "Controlador local pasa al estado seguro definido", "Evento y recuperacion"],
        ["14", "Entrega", "Actualizar planos, parametros, BOM y etiquetas", "Paquete as-built firmado y copia en gabinete", "Acta de entrega"],
    ]
    draw_table(c, 35, 720, [55,170,300,410,185], headers, rows, 7.6, row_height=39)
    box(c, 35, 82, 1120, 70, "SECUENCIA DE ENERGIZACION",
        "La primera energizacion de 110/220 VAC requiere electricista competente. El primer arranque del compresor requiere ademas tecnico frigorista. Las pruebas de firmware se ejecutan primero con salidas de baja tension o cargas simuladas; no se usa el rele Waveshare para puentear protecciones.",
        LIGHT_RED, RED, body_size=10.2)


def references(c):
    page_frame(c, "E-108", "FUENTES, REVISION Y FIRMAS", 9)
    box(c, 40, 535, 710, 185, "FUENTES TECNICAS UTILIZADAS",
        "[S1] Documento del propietario: DOCUMENTO_DIAGRAMA_TECNICO_ELECTRICO.md, 2026-09-13.\n"
        "[S2] Firmware LVGL src/main.cpp y src/display_config.h, revision de trabajo 2026-09-13.\n"
        "[S3] Esquema Waveshare ESP32-S3-Relay-6CH suministrado: relay_schematic.pdf.\n"
        "[S4] Waveshare ESP32-S3-Relay-6CH: https://www.waveshare.com/wiki/ESP32-S3-Relay-6CH\n"
        "[S5] Waveshare ESP32-S3-Touch-LCD-4.3B: https://docs.waveshare.com/ESP32-S3-Touch-LCD-4.3B\n"
        "[S6] Pedrollo PKm 60, ficha 60 Hz: https://www.pedrollo.com/wp-content/uploads/schede-tecniche/EN/PKm-60_EN-datasheet_60Hz.pdf\n"
        "[S7] Fotografias de campo entregadas por el propietario: bomba, VFD, transformador, chiller y planta.",
        LIGHT_BLUE, BLUE, body_size=9.5)
    box(c, 785, 535, 365, 185, "DOCUMENTOS A ANEXAR",
        "- Manual exacto VFD PD2000 3 32.\n"
        "- Placa y hoja del compresor.\n"
        "- Placa completa T1 y diagrama.\n"
        "- Placas YV1/YV2.\n"
        "- Unifilar de acometida y tierra.\n"
        "- Calculo de protecciones/conductores.\n"
        "- Matriz causa-efecto.\n"
        "- Plano as-built y backup VFD.",
        LIGHT_AMBER, AMBER, body_size=10)

    headers = ["REV", "FECHA", "CAMBIO", "RESPONSABLE", "ESTADO"]
    rows = [
        ["A", "2026-09-13", "Emision preliminar basada en fotografias, firmware y fichas oficiales", "SDBrewPi / por validar", "REVISION"],
        ["B", "", "Datos de campo, manual VFD y calculos incorporados", "", "PENDIENTE"],
        ["C", "", "Planos de taller aprobados", "", "PENDIENTE"],
        ["AS-BUILT", "", "Cambios de obra, pruebas y parametros finales", "", "PENDIENTE"],
    ]
    draw_table(c, 40, 485, [75,115,500,250,170], headers, rows, 8.5, row_height=42)

    box(c, 40, 100, 350, 155, "ELABORO / REVISO",
        "Nombre: ______________________________\n\nMatricula: ____________________________\n\nFirma: _______________________________\n\nFecha: _______________________________",
        WHITE, NAVY, body_size=10)
    box(c, 420, 100, 350, 155, "ELECTRICISTA INSTALADOR",
        "Nombre: ______________________________\n\nMatricula: ____________________________\n\nFirma: _______________________________\n\nFecha: _______________________________",
        WHITE, NAVY, body_size=10)
    box(c, 800, 100, 350, 155, "TECNICO FRIGORISTA / ENTREGA",
        "Nombre: ______________________________\n\nEmpresa: _____________________________\n\nFirma: _______________________________\n\nFecha: _______________________________",
        WHITE, NAVY, body_size=10)


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=PAGE, pageCompression=1)
    c.setTitle("SDBrewPi E-101 - Paquete electrico preliminar")
    c.setAuthor("SDBrewPi")
    for fn in [cover, power_diagram, control_diagram, instrumentation, terminals,
               bom, holds, commissioning, references]:
        fn(c)
        c.showPage()
    c.save()
    print(OUTPUT)


if __name__ == "__main__":
    main()
