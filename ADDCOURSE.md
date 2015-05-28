
## Documentation adding a new course

1. Trace holes in Google Earth
2. Export course as KML to `dogleg-courses/us/...`
3. Use web interface to create a new course with rating info
4. Download raw JSON of new course via `http://localhost:9000/courses/raw/<course_id>`
5. Save raw JSON content along side course KML file
6. `activator "run-main utils.AddElevation <course_name>"`
7. `activator "run-main utils.GenerateKmlFromJson <course_name>"`
8. Commit new KML/JSON files, close issue (if needed).
9. Mark any related e-mails (from request) as done.
