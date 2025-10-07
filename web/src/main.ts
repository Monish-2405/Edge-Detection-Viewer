const img = document.getElementById("frame") as HTMLImageElement;
const fpsEl = document.getElementById("fps") as HTMLElement;
const resEl = document.getElementById("res") as HTMLElement;

async function update() {
  try {
    const r = await fetch('/api/frame');
    const j = await r.json();
    img.src = j.base64;
    fpsEl.textContent = String(j.fps ?? '--');
    resEl.textContent = `${j.width ?? 0}x${j.height ?? 0}`;
  } catch {
    // ignore
  }
}

update();


