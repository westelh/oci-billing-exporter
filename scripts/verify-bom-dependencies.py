"""Compare strict Gradle resolution before and after the BOM declaration cleanup."""
import argparse
import difflib
import json
import os
from pathlib import Path
import subprocess
import tarfile
import tempfile

CONFIGURATIONS = (
    "compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath"
)
ADDED_BOMS = {
    "io.prometheus:prometheus-metrics-bom:1.4.1",
    "org.slf4j:slf4j-bom:2.0.16",
    "org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.9.0",
    "io.kotest:kotest-bom:5.9.1",
}


def compare(before, after):
    if set(before) != set(CONFIGURATIONS) or set(after) != set(CONFIGURATIONS):
        raise ValueError("Snapshot must contain exactly the four classpaths")
    equal = True
    for name in CONFIGURATIONS:
        for kind in ("artifacts", "components"):
            old = set(before[name][kind])
            new = set(after[name][kind])
            # Only newly added metadata for these exact BOM versions is allowed.
            if kind == "components":
                new -= ADDED_BOMS - old
            if old != new:
                equal = False
                print(f"{name}: {kind} differ")
                print("".join(difflib.unified_diff(
                    [x + "\n" for x in sorted(old)],
                    [x + "\n" for x in sorted(new)],
                    fromfile="before", tofile="after")), end="")
        print(f"{name}: {len(before[name]['artifacts'])} -> "
              f"{len(after[name]['artifacts'])} library coordinates")
    return equal


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", help="Git revision of main before the cleanup")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = root / "build/bom-verification"
    output.mkdir(parents=True, exist_ok=True)
    baseline = subprocess.check_output(
        ["git", "rev-parse", "--verify", args.baseline + "^{commit}"], cwd=root, text=True
    ).strip()
    (output / "baseline.txt").write_text(baseline + "\n")
    environment = dict(os.environ, GRADLE_USER_HOME=str(root / ".gradle-user-home"))
    with tempfile.TemporaryDirectory(prefix="bom-baseline-") as temporary:
        archive = output / "baseline.tar"
        with archive.open("wb") as stream:
            subprocess.run(["git", "archive", baseline], cwd=root, stdout=stream, check=True)
        with tarfile.open(archive) as contents:
            contents.extractall(temporary, filter="data")
        archive.unlink()
        for label, project in (("before", temporary), ("after", str(root))):
            with (output / f"{label}.log").open("w") as log:
                subprocess.run([
                    "bash", str(Path(project) / "gradlew"), "--no-daemon", "--console=plain",
                    "-p", project, "-I", str(root / "scripts/dependency-snapshot.init.gradle"),
                    f"-DdependencySnapshot.output={output / (label + '.json')}",
                    "dependencySnapshot",
                ], cwd=root, env=environment, stdout=log, stderr=subprocess.STDOUT, check=True)
    before = json.loads((output / "before.json").read_text())
    after = json.loads((output / "after.json").read_text())
    if not compare(before, after):
        raise SystemExit(1)
    print(f"PASS: all four classpaths match baseline {baseline}; only BOM metadata additions allowed")


if __name__ == "__main__":
    main()
