package indigo

import kotlin.system.exitProcess

fun main() {
    Indigo().play()
}

class Indigo(){

    private var deck = createDeck().shuffled().toMutableList()
    private var cardsOnTheTable = deck.take(4).toMutableList()
    private val onePointList = listOf("A", "10", "J", "Q", "K")
    private var playerScore = 0
    private var computerScore = 0
    private var whoWon = ""
    private var whoStarted = ""
    private lateinit var playersCards: MutableList<String>
    private lateinit var aiCards: MutableList<String>
    private var playerCardsWon = mutableListOf<String>()
    private var aiCardsWon = mutableListOf<String>()
    private var candidatesListSuits = mutableListOf<String>()
    private var candidatesListRanks = mutableListOf<String>()
    private var commonCandidates = mutableListOf<String>()
    private var initial = true
    private val regex = Regex("♦|♥|♠|♣")

    init {
        initGame()
        Messages.GameLabel.print()
    }

    fun play(){
        Messages.OrderQuestion.print()
        while (deck.size != 0){
            when( readln().lowercase() ){
                Commands.YES.command -> {
                    whoStarted = "p"
                    printInitial()
                    engine(::playerMove, ::aiMove)
                }
                Commands.NO.command -> {
                    whoStarted = "c"
                    printInitial()
                    engine(::aiMove, ::playerMove)
                }
                else -> Messages.OrderQuestion.print()
            }
        }
        finalizePoints()
        printInfo()
        printFinalScore()
        Messages.GameOver.print()
    }

    private fun engine(f1: () -> Unit, f2: () -> Unit){
        for(i in 1..4){
            for(j in 1..6){
                f1()
                f2()
            }
            initGame()
        }
    }

    private fun initGame(){
        updateDeck(cardsOnTheTable)
        playersCards = hand6Cards()
        updateDeck(playersCards)
        aiCards = hand6Cards()
        updateDeck(aiCards)
    }

    private fun hand6Cards() = deck.take(6).toMutableList()

    private fun playerMove(){
        printInfo()
        printList(playersCards)
        val end = playersCards.size
        while(true){
            printOptions()
            val regex = Regex("[1-$end]")
            val userInput = readln()
            if(userInput.lowercase() == Commands.EXIT.command){
                Messages.GameOver.print()
                exitProcess(0)
            }
            if(regex.matches(userInput)){
                val index = userInput.toInt() - 1
                val card = playersCards[index]
                updateCardsOnTheTable(playersCards[index])
                playersCards.removeAt(index)
                if(checkForWin(card))
                    processWin()
                break
            }
            else continue
        }
    }

    private fun aiMove(){
        printInfo()
        val card = aiBrain()
        clearCandidates()
        updateCardsOnTheTable(card)
        printAICards()
        println("Computer plays $card")
        aiCards.remove(card)
        if(checkForWin(card)){
            processAiWin()
        }
    }

    private fun processWin() {
        updatePlayersCard(playerCardsWon)
        playerScore = calculatePoints(playerScore)
        printScore("Player")
        cardsOnTheTable.clear()
        whoWon = "p"
    }

    private fun processAiWin() {
        updatePlayersCard(aiCardsWon)
        computerScore = calculatePoints(computerScore)
        printScore("Computer")
        cardsOnTheTable.clear()
        whoWon = "c"
    }

    private fun finalizePoints(){
        when(whoWon){
            "p" -> {
                playerScore = calculatePoints(playerScore)
                updatePlayersCard(playerCardsWon)
            }
            "c" -> {
                computerScore = calculatePoints(computerScore)
                updatePlayersCard(aiCardsWon)
            }
        }
        if(playerCardsWon.size > aiCardsWon.size)
            playerScore += 3
        else if(playerCardsWon.size < aiCardsWon.size)
            computerScore += 3
        else{
            if(whoStarted == "p")
                playerScore += 3
            else
                computerScore += 3
        }
    }
    private fun printInfo() = if(cardsOnTheTable.size == 0) println("\nNo cards on the table")  else println("\n${cardsOnTheTable.size} cards on the table, and the top card is ${cardsOnTheTable.last()}")

    private fun printInitial()  {
        if(initial){
            print("Initial cards on the table: ")
            cardsOnTheTable.forEach { print("$it ") }
            println()
            initial = !initial
        }
    }

    private fun createDeck(): MutableList<String> {
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")
        val suits = listOf("♦", "♥", "♠", "♣")
        return suits.flatMap { suit -> ranks.map{it + suit} }.toMutableList()
    }

    private fun printList(list: MutableList<String>) {
        print("Cards in hand: ")
        list.forEachIndexed{ i, it -> print("${i + 1})$it ")  }
        println()
    }

    private fun printAICards(){
        aiCards.forEach { print("$it ") }
        println()
    }


    private fun printOptions() = println("Choose a card to play (1-${playersCards.size}):")

    private fun updateDeck(cards: List<String>) = deck.removeAll(cards)

    private fun updateCardsOnTheTable(card: String) = cardsOnTheTable.add(card)

    private fun updatePlayersCard(list: MutableList<String>) = list.addAll(cardsOnTheTable)

    private fun calculatePoints(  number: Int ): Int{
        var score = number
        onePointList.forEach { points -> cardsOnTheTable.forEach {
            if(points.contains(it.split(regex).first()) || points.contains(it.last()))
                score++ }}
        return score
    }

    private fun checkForWin(card: String): Boolean {
        try {
            val topCard = cardsOnTheTable.dropLast(1).last()
            val topRank = topCard.split(regex).first()
            val topSuit = topCard.last()
            val rank = card.split(regex).first()
            val suit = card.last()
            if(topRank == rank || topSuit == suit )
                return true
        } catch(_: NoSuchElementException){
        }

        return false
    }

    private fun aiBrain(): String {
        if(cardsOnTheTable.isEmpty()) return throwCards()
        getCandidates()
        return when{
            aiCards.size == 1 ->  aiCards[0]
            candidatesListSuits.isEmpty() && candidatesListRanks.isEmpty() && commonCandidates.isEmpty() -> throwCards()
            candidatesListSuits.isNotEmpty() -> candidatesListSuits.random()
            candidatesListRanks.isNotEmpty() -> candidatesListRanks.random()
            else -> commonCandidates.random()
        }
    }

    private fun throwCards(): String {
        val sameSuits = aiCards.groupingBy { it.last() }.eachCount().filterValues { it > 1 }
        val sameRanks = aiCards.groupingBy { it.split(regex)[0] }.eachCount().filterValues { it > 1 }
        val sameSuitsList = aiCards.filter { it.last() in sameSuits.keys }
        val sameRanksList = aiCards.filter {  it.dropLast(1) in sameRanks.keys }
        return when {
            sameSuitsList.isNotEmpty() -> sameSuitsList.random()
            sameRanks.isEmpty() -> sameRanksList.random()
            else -> aiCards.random()
        }
    }

    private fun clearCandidates(){
        commonCandidates.clear()
        candidatesListRanks.clear()
        candidatesListSuits.clear()
    }
    private fun getCandidates() {
        val topCard = cardsOnTheTable.last()
        val topSuit = topCard.last()
        val topRank = topCard.split(regex)[0]
        val listOfSuits = aiCards.map { it.last() }
        val listOfRanks = aiCards.map { it.split(regex)[0] }
        val sameSuits = listOfSuits.count { it == topSuit }
        val sameRanks = listOfRanks.count { it == topRank }
        when {
            sameSuits >= 2 -> candidatesListSuits = aiCards.filter { it.last() == topSuit }.toMutableList()
            sameRanks >= 2 -> candidatesListRanks = aiCards.filter { it.split(regex)[0] == topRank }.toMutableList()
            else -> aiCards.forEach { if(it.last() == topSuit || it.split(regex)[0] == topRank) commonCandidates.add(it) }

        }
    }

    private fun printScore(name: String) = println("$name wins cards\n" +
                                                    "Score: Player $playerScore - Computer $computerScore\n" +
                                                    "Cards: Player ${playerCardsWon.size} - Computer ${aiCardsWon.size}")

    private fun printFinalScore() = println("Score: Player $playerScore - Computer $computerScore\n" +
                                            "Cards: Player ${playerCardsWon.size} - Computer ${aiCardsWon.size}")

}

enum class Commands(val command: String, private val message: String) {
    YES("yes", ""),
    NO("no", ""),
    EXIT("exit", "Game Over");
}

enum class Messages(private val msg: String){
    GameOver("Game Over"),
    OrderQuestion("Play first?"),
    GameLabel("Indigo Card Game");
    fun print() {
        println(this.msg)
    }
}
