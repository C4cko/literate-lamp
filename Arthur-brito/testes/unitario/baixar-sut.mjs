/**
 * Baixa, no commit fixado pela turma, só as classes do Buzz exercitadas
 * pelos testes unitários. O restante do OrangeHRM não entra no repositório.
 */
import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const COMMIT = "56e23b3b09e7af29317a0943523b825843fff527";
const BASE = `https://raw.githubusercontent.com/orangehrm/orangehrm/${COMMIT}`;

const ARQUIVOS = {
  "TextHelperService.php":
    "src/plugins/orangehrmCorePlugin/Service/TextHelperService.php",
  "InvalidURLException.php":
    "src/plugins/orangehrmBuzzPlugin/Exception/InvalidURLException.php",
  "AbstractBuzzVideoURL.php":
    "src/plugins/orangehrmBuzzPlugin/Dto/BuzzVideoURL/AbstractBuzzVideoURL.php",
  "BuzzEmbeddedURL.php":
    "src/plugins/orangehrmBuzzPlugin/Dto/BuzzVideoURL/BuzzEmbeddedURL.php",
  "EmbeddedURLForYoutube.php":
    "src/plugins/orangehrmBuzzPlugin/Dto/BuzzVideoURL/EmbeddedURLForYoutube.php",
  "EmbeddedURLForVimeo.php":
    "src/plugins/orangehrmBuzzPlugin/Dto/BuzzVideoURL/EmbeddedURLForVimeo.php",
  "EmbeddedURLForDailymotion.php":
    "src/plugins/orangehrmBuzzPlugin/Dto/BuzzVideoURL/EmbeddedURLForDailymotion.php",
};

const destino = path.join(path.dirname(fileURLToPath(import.meta.url)), "sut");
await mkdir(destino, { recursive: true });

for (const [nome, remoto] of Object.entries(ARQUIVOS)) {
  const resposta = await fetch(`${BASE}/${remoto}`);
  if (!resposta.ok) {
    throw new Error(`Falha ao baixar ${remoto}: HTTP ${resposta.status}`);
  }
  await writeFile(path.join(destino, nome), await resposta.text(), "utf8");
  console.log(nome);
}

console.log(`Classes do commit ${COMMIT} em ${destino}`);
