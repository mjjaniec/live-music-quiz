# Live Music Quiz

## What is that?
Live music quiz is a social event organized by so-called **maestro** - the quiz host.
Maestro is the organizer and conductor of the event, they invite **players** to join the quiz and 
**performer** (a person on group) to play music questions. **Maestro** and **performer** may be the same person.
BTW **quiz** can also be referred as **game**

## Where

In a **venue**, venue has to provide: 
 * space for **performer** to play music live 
 * a private **maestro-screen** for **maestro** to lead the quiz
 * a public **big-screen** also managed by **maestro** but visible to all the players
 * seats for players, players must be able to see **big-screen** and hear **performer** playing

## How

 * **Maestro** chooses the **venue**, invites **players** and **performer** (that may be the same person as **maestro**)
 * **Maestro** and **performer** come first to the **venue**
 * **Performer** prepares music instruments for live playing
 * **Maestro** uses **maestro-screen** to start up the game and displays on **big-scrin** a welcome message with link for the players to join the game.
 * **Players** come to the venue, use their smartphones to join the game. They scan QR code form **big-screen** to open link to game
 * Once **Players** are gathered **maestro** starts the first **round** of the game
 * **Round** is a set of music questions asked with the same rules (more in next chapter)
 * For each question in a round **performer** plays an excerpt from a (more or less) well known **music piece** (a song, movie theme etc..)
 * **Players** use their smartphones to provide the answer - original artist and title of the piece played
 * When a round is finished, **maestro** shows summary - a table on **big-screen** with ranking of players with their points
 * After last round there is **play-off** (more in next chapter)
 * After that **maestro** reveals final results on the **big-screen**, gives awards to the best players, **players** on their phones see a feedback view.
 * Then the game is finished, and all participants leave the **venue**

## More about **round** and **play-off**

A **Round** can be played in one of 3 modes 
   * mode **everybody**
     - **maestro** chooses the **music piece**,
     - **performer** play an excerpt from that piece (**performer** can see **maestro** screen so knows what to play)
     - **players** see on their phones a form to fill with **Artist** and **Title** fields.
     - Each field has hints to select from (there are thousands of options to choose, so hints don't make it easier to guess, players type answers and hints are fuzzy filtered). There is also "don't know" option to select. Once answers are selected they confirm, after that then they cannot edit the answer. 
     - On the **big-screen** players see who already answered
     - when **maestro** decides (usually after all **players** answered), they reveal the correct answer on **big-screen**
     - **players** can see the correct answer on **big-screen** and their points on their phones
     - each field (**artist** and **title**) are checked separately and points are added for each **player**
   * mode **onion**
     - very similar to mode **everybody** with the difference that quicker **players** get more points.
     - first correct answer for given **piece** field (**artist** or **title**) give max points, following correct answers give decreasing amount of points
     - the mode is called **onion** because an **onion** has layers as well as **music piece** - it is supposed to be **performed** starting with 
       one music line and then slowly adding oter (e.g. starting with bassline only, then adding drums, then keys and then guitar)
     - the more layers of music are played then it's easier to guess that's why earlier guesses are promoted
   * mode **first**
     - again **maestro** chooses a **piece** to be played
     - **players** on their phones do not see form as above cases but a button to apply for answering
     - when first **player** hits that button, all others are blocked, the first player is displayed on **big-screen**, 
       the **performer** stops playing and **maestro** asks aloud (using speach, not the application) for the answer 
     - **maestro** judges the answer and either accepts it (finishing the piece ) or reject it.
     - if the answer is (partially) incorrect, the game continue, **performer** resumes play, remain players can apply again.
     - it continues until correct answers are provided, or **maestro** decides to end the piece and reval the answer on **big-screen**
     - in this mode each incorrect answers bums the pool of point to get for next **player** (in context of single **music piece**)
     
The **Play-off** is additional question for players that is designed to break any possible tie. Additionally,
    **maestro** can assign an award for best **play-off**. The play-off is estimation task that is intentionally very hard to guess.
    **Performer** plays a single piece once, **players** have to input number of music notes that are were played on their phones


## Quiz preparation

Before Live Music Quiz event can happen **maestro** prepares **set-list** - list of rounds, and for each round list of music pieces. 
Typically, a **set-list** have 3-6 rounds and each round contain 4-12 pieces. 
**Maestro** can also prepare **test-setlist** very short quiz that will be used to familiarize **players** with quiz mechanics.
**Performer** must know the **set-list** and be ready to perform all the pieces live for the **players**

# The project

Project provides tools for **maestro** and players. 
It is a web service that provide separate vies for **maestro**, **big-screen** and **players**
It also can read **set-list** from a Google spreadsheet (there is no proper content management within application)

# Change plan

 * Currently, everything is global and has no proper access control
 * I want to add user accounts for **maestro** (registration & login)
 * I want to scope the game in **maestro** account, alo scope set-list and to a particular game
 * (Optionally) I want to create a separate view for **performer** so they can see what to play without using **maestro** screen



