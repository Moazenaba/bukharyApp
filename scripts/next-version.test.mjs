import assert from 'node:assert/strict';
import test from 'node:test';
import { nextVersion } from './next-version.mjs';

const gradle = (code, name) => `defaultConfig {\n    versionCode = ${code}\n    versionName = "${name}"\n}`;

test('nothing published yet: uses the gradle version', () => {
  assert.deepEqual(nextVersion(gradle(12, '1.1.10'), null), { code: 12, name: '1.1.12' });
});

test('published equals gradle: goes one higher (the case that used to fail)', () => {
  assert.deepEqual(nextVersion(gradle(12, '1.1.10'), { versionCode: 12 }), { code: 13, name: '1.1.13' });
});

test('published is higher than gradle: follows the published one', () => {
  assert.deepEqual(nextVersion(gradle(5, '1.1.3'), { versionCode: 20 }), { code: 21, name: '1.1.21' });
});

test('gradle is higher than published: keeps gradle (a deliberate jump)', () => {
  assert.deepEqual(nextVersion(gradle(30, '1.2.0'), { versionCode: 12 }), { code: 30, name: '1.2.30' });
});

test('missing version in gradle is an error', () => {
  assert.throws(() => nextVersion('nothing here', null));
});
