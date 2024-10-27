import express from 'express';
import cors from 'cors';
import fetch from 'node-fetch';
import { deflateSync } from 'zlib';
import { Buffer } from 'buffer'; // Buffer 모듈 사용

const app = express();
const PORT = 5000;

app.use(cors());
app.use(express.json());

const encodeUml = (umlCode) => {
    // UML 코드를 UTF-8로 인코딩
    const buffer = Buffer.from(umlCode, 'utf-8');

    // Deflate 알고리즘을 사용하여 압축
    const compressed = deflateSync(buffer); // deflateRawSync 사용

    // Custom Base64 인코딩
    const base64 = encodeCustomBase64(compressed);

    // URL에 사용할 수 있도록 변환
    return `~1${base64}`; // ~1 헤더 추가
};

// Custom Base64 인코딩 함수
const encodeCustomBase64 = (data) => {
    let r = "";
    for (let i = 0; i < data.length; i += 3) {
        if (i + 2 == data.length) {
            r += append3bytes(data[i], data[i + 1], 0);
        } else if (i + 1 == data.length) {
            r += append3bytes(data[i], 0, 0);
        } else {
            r += append3bytes(data[i], data[i + 1], data[i + 2]);
        }
    }
    return r;
};

const append3bytes = (b1, b2, b3) => {
    const c1 = b1 >> 2;
    const c2 = ((b1 & 0x3) << 4) | (b2 >> 4);
    const c3 = ((b2 & 0xF) << 2) | (b3 >> 6);
    const c4 = b3 & 0x3F;
    let r = "";
    r += encode6bit(c1 & 0x3F);
    r += encode6bit(c2 & 0x3F);
    r += encode6bit(c3 & 0x3F);
    r += encode6bit(c4 & 0x3F);
    return r;
};

const encode6bit = (b) => {
    if (b < 10) {
        return String.fromCharCode(48 + b); // 0-9
    }
    b -= 10;
    if (b < 26) {
        return String.fromCharCode(65 + b); // A-Z
    }
    b -= 26;
    if (b < 26) {
        return String.fromCharCode(97 + b); // a-z
    }
    b -= 26;
    if (b === 0) {
        return '-'; // -
    }
    if (b === 1) {
        return '_'; // _
    }
    return '?'; // 잘못된 값
};

app.post('/generate-uml', async (req, res) => {
    const umlCode = req.body.umlCode;
    const encodedUml = encodeUml(umlCode);
    const url = `https://www.plantuml.com/plantuml/png/${encodedUml}`;

    try {
        const response = await fetch(url);

        if (!response.ok) {
            console.error('Failed to fetch from PlantUML:', response.status, response.statusText);
            throw new Error(`Network response was not ok: ${response.statusText}`);
        }

        const arrayBuffer  = await response.arrayBuffer();
        const imageBuffer = await Buffer.from(arrayBuffer);
        res.set('Content-Type', 'image/png');
        res.send(imageBuffer); // PNG 이미지 전송

    } catch (error) {
        console.error('Error generating UML:', error);
        res.status(500).send('Error generating UML: ' + error.message);
    }
});

app.listen(PORT, () => {
    console.log(`Server running on http://localhost:${PORT}`);
});
