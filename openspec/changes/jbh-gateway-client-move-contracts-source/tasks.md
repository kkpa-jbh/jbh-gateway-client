## 1. Test blueprint

Does not apply: no unit tests (user decision, JBH-74) and no Java code change. No blueprint group.

## 2. Contracts source (independent sub-tasks, no dependencies between them)

- [ ] 2.1 `pom.xml`: in profile `github`, change the repository URL (line 191) to `https://maven.pkg.github.com/kkpa-jbh/jbh-notification`. Update the comment above the profile (line 184) to say the contracts are published by the `jbh-notification` repo. Do not change dependency coordinates. (D1)
- [ ] 2.2 `.github/workflows/publish.yml`: update the header comment (line 2): contracts come from `jbh-notification`, workflow `publish`. (D1)
- [ ] 2.3 `README.md` "Install and publish": change lines 167, 170 and 171 (contracts source `jbh-personal-finance` to `jbh-notification`; CI order step `publish-contracts` to `publish`). Leave lines 31, 32, 166 and the "images" part of line 171 (`jbh-personal-finance` is a consumer there). (D2)

## 3. Verification (depends on 2.1, 2.2, 2.3)

- [ ] 3.1 Search `grep -rn "jbh-personal-finance" . --exclude-dir=.git --exclude-dir=target`. Only the non-contracts hits listed in design.md remain (`README.md` lines 31, 32, 166, 171 images part). `pom.xml` and `.github/` have 0 hits. (D2)
- [ ] 3.2 Expected counts: 0 hits of `maven.pkg.github.com/kkpa-jbh/jbh-personal-finance`; 1 hit of `maven.pkg.github.com/kkpa-jbh/jbh-notification` in `pom.xml`. (D3)

Size estimate: 3 files, ~10 changed lines (tests and docs included)
