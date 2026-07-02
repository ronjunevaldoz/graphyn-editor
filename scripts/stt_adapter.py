import argparse
import json
import sys
import whisper


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--audio", required=True)
    parser.add_argument("--language", default=None)
    parser.add_argument("--model", default="base")
    args = parser.parse_args()

    try:
        model = whisper.load_model(args.model)

        result = model.transcribe(
            args.audio,
            language=args.language if args.language else None,
        )

        output = {
            "text": result.get("text", "").strip(),
            "language": result.get("language"),
            "segments": [
                {
                    "start": segment.get("start"),
                    "end": segment.get("end"),
                    "text": segment.get("text", "").strip(),
                }
                for segment in result.get("segments", [])
            ],
        }

        print(json.dumps(output, ensure_ascii=False))
        return 0

    except Exception as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())