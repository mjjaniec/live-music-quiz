declare var autoComplete: any;
const EXPECTED_HINTS = 5;

interface Hint {
    match: string;
    value: string;
}

function occurrences(string: string, subString: string): number {
    string += "";
    subString += "";
    if (subString.length <= 0) return (string.length + 1);

    var n = 0, pos = 0, step = subString.length;

    while (true) {
        pos = string.indexOf(subString, pos);
        if (pos >= 0) {
            ++n;
            pos += step;
        } else break;
    }
    return n;
}

function hintPoints(hint: Hint): number {
    const start = hint.match.startsWith("<mark>") ? 2 : 1;
    const parts = occurrences(hint.match, "<mark>");
    const byParts = (parts === 1) ? 10 : Math.max(6 - parts, 1);
    return byParts * start;
};


function processHints(hints: Hint[]): Hint[] {
    const byPoints = new Map<number, Hint[]>();
    const simplified = hints.map(hint => ({ ...hint, match: hint.match.replaceAll(/<[/]mark><mark>/g, "") }));
    simplified.forEach(hint => {
        const points = hintPoints(hint);
        if (!byPoints.has(points)) {
            byPoints.set(points, []);
        }
        byPoints.get(points)?.push(hint);
    });

    const orderedPoints: number[] = Array.from(byPoints.keys()).sort((a, b) => b - a);
    const processedHints: Hint[] = [];
    for (const points of orderedPoints) {
        const array = byPoints.get(points) || [];
        array.slice(0, EXPECTED_HINTS - processedHints.length)?.forEach(h => processedHints.push(h));
        if (processedHints.length === EXPECTED_HINTS) {
            break;
        }
    }
    return processedHints;
}

function setupAutocomplete(elementId: string, placeHolder: string, fetchApiPath: string, dunnoOption: string) {
    const config = {
        selector: `#${elementId}`,
        placeHolder: placeHolder,
        data: {
            cache: true,
            filter: processHints,
            src: async (query: string) => {
                try {
                    const source = await fetch(fetchApiPath);
                    return await source.json();
                } catch (error) {
                    return error;
                }
            }
        },
        searchEngine: "loose",
        resultItem: {
            highlight: true
        },
        diacritics: true,
        events: {
            input: {
                selection: (event: any) => {
                    const index = event.detail.selection.index;
                    const selection = index === 0 ? dunnoOption : event.detail.matches[index - 1].value;
                    element.input.value = selection;
                    element.input.blur();
                },
                focus() {
                    if (element.input.value === dunnoOption) {
                        element.input.value = "";
                    }
                    element.start();
                }
            }
        },
        trigger: (query: string) => {
            return true;
        },
        resultsList: {
            element: (list: any, data: any) => {
                if (true) {
                    const message = document.createElement("li");
                    message.setAttribute("class", "dunno");
                    message.innerText = dunnoOption;
                    list.prepend(message);
                }
            },
            noResults: true,
        }
    };
    const element = new autoComplete(config);
    element.input.setAttribute("autocomplete", "off");
}


(window as any).setupAutocomplete = setupAutocomplete;
