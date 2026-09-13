// Sprint 25: turns the JSON that backend/scripts/seed_test_session.py prints on stdout into a
// Postman environment file Newman can load with `-e`. Kept as a tiny script rather than a shell
// one-liner because Postman environments have a specific nested shape (an array of
// {key, value} objects, not a flat map) that's easier to get right in real code.
const seed = JSON.parse(process.argv[2]);

const baseUrl = process.argv[3] || "http://localhost:8000/api/v1";

const environment = {
  name: "netprotect-ci",
  values: [
    { key: "baseUrl", value: baseUrl, enabled: true },
    { key: "tutorAccessToken", value: seed.tutor_access_token, enabled: true },
    { key: "supervisedAccessToken", value: seed.supervised_access_token, enabled: true },
    {
      key: "strangerTutorAccessToken",
      value: seed.stranger_tutor_access_token,
      enabled: true,
    },
  ],
};

process.stdout.write(JSON.stringify(environment, null, 2));
