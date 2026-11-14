// Works out the version of the next release so nobody has to raise it by hand.
//
//   node scripts/next-version.mjs app/build.gradle.kts web/public/app/android.json
//
// versionCode = the larger of the one in build.gradle.kts and (the published one + 1), so a second
// press of "Publish Android" never fails with "versionCode is not newer".
// versionName = the first two numbers of the one in build.gradle.kts, then the versionCode
// (1.1.10 in gradle, 12 published -> 1.1.13 with code 13).
// Prints `code=...` and `name=...` (also appended to $GITHUB_OUTPUT when it is set).
import fs from 'node:fs';
import { pathToFileURL } from 'node:url';

export function nextVersion(gradleText, published) {
  const code = Number(gradleText.match(/versionCode\s*=\s*(\d+)/)?.[1]);
  const name = gradleText.match(/versionName\s*=\s*"([^"]+)"/)?.[1];
  if (!Number.isInteger(code) || !name) throw new Error('versionCode / versionName not found in build.gradle.kts');
  const publishedCode = Number(published?.versionCode) || 0;
  const next = Math.max(code, publishedCode + 1);
  const base = name.split('.').slice(0, 2).join('.');
  return { code: next, name: `${base}.${next}` };
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  const [gradlePath, manifestPath] = process.argv.slice(2);
  if (!gradlePath) {
    console.error('Usage: node scripts/next-version.mjs app/build.gradle.kts [path/to/android.json]');
    process.exit(1);
  }
  const published = manifestPath && fs.existsSync(manifestPath) ? JSON.parse(fs.readFileSync(manifestPath, 'utf8')) : null;
  const { code, name } = nextVersion(fs.readFileSync(gradlePath, 'utf8'), published);
  const lines = `code=${code}\nname=${name}\n`;
  process.stdout.write(lines);
  if (process.env.GITHUB_OUTPUT) fs.appendFileSync(process.env.GITHUB_OUTPUT, lines);
}
