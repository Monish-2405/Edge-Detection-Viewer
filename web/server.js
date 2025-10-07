const express = require('express');
const path = require('path');
const app = express();

app.use('/dist', express.static(path.join(__dirname, 'dist')));
app.use('/public', express.static(path.join(__dirname, 'public')));

// Dummy 1x1 black PNG base64
const dummy = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAukB9VQqU8EAAAAASUVORK5CYII=';

app.get('/api/frame', (_req, res) => {
  res.json({ base64: dummy, fps: '--', width: 1, height: 1 });
});

app.get('/', (_req, res) => {
  res.redirect('/public/index.html');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`web viewer on http://localhost:${PORT}`));


