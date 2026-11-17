import { SessaoOrangeHrm } from "./cliente.js";

const sessao = new SessaoOrangeHrm();
await sessao.entrar();
const feed = await sessao.api("/api/v2/buzz/feed?limit=50");
const meus = (feed.corpo?.data ?? []).filter(
  (item) => typeof item.text === "string" && item.text.startsWith("AB-"),
);
for (const item of meus) {
  const del = await sessao.api(`/api/v2/buzz/shares/${item.id}`, { method: "DELETE" });
  console.log("apagado", item.id, item.text.slice(0, 48), del.status);
}
if (meus.length === 0) console.log("nada para apagar");
