package com.archmage.jobsheetmaker.model

import java.time.LocalDate
import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import java.time.Duration
import com.archmage.jobsheetmaker.Tools
import java.io.InputStream
import java.io.FileInputStream
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime

object Job {
	def template: InputStream = Tools.getFirstExistingStream(
		Tools.getStreamIfFileExists(new File("Single Job Template.pdf")),
		WorkDay.getClass.getResourceAsStream("/Single Job Template.pdf"),
		Tools.getStreamIfFileExists(new File("src/main/resources/Single Job Template.pdf"))) // final line is debugging

	//	def template = { new File("src/main/resources/Single Job Template.pdf") }
	//	val template = WorkDay.getClass.getResourceAsStream("/Single Job Template.pdf")
}

class Job(
	val client: Client,
	private val _worker: Array[Worker],
	val datetime: LocalDateTime,
	val duration: Duration,
	val services: String,
	val confirmed: String = "",
	val _comments: String = "",
	val cancelled: Boolean = false) {

	var comments = _comments

	def worker = { _worker }
	val date = datetime.toLocalDate

	override def toString = {
		var status = if (cancelled) "[CANCELLED] " else ""
		if (status == "" && confirmed == "") status += "[UNCONFIRMED] "
		s"$status$date ${client.name} - ${worker(0)}, $services"
	}

	def durationAsString = {
		val hours = duration.toHours()
		val minutes = duration.minusHours(hours).toMinutes()
		//		s"${hours}h ${minutes}m"
		s"${hours}:${if (minutes < 10) "0" else ""}${minutes}"
	}

	def outputJobsheet = {
		val document = PDDocument.load(Job.template)
		val acroForm = document.getDocumentCatalog().getAcroForm()
		val fieldNames = Array("Title", "Client", "Contact", "Confirmed", "Address", "Comments", "Duration")
		val values = Array(s"${client.name} - $services on ${
			date.format(DateTimeFormatter.ofPattern("EEEE dd/MM/uuuu"))
		} ", client.name, client.phone,
			confirmed, client.address.toString(), comments, durationAsString)
		for (i <- 0 to fieldNames.length - 1) {
			WorkDay.setField(acroForm, fieldNames(i), values(i))
		}

		document
	}

	def exportJobsheet = {
		outputJobsheet.save(s"${worker(0)} - Job for ${client.name}, $date.pdf")
		outputJobsheet.close()
	}

	def checkEquality(other: Job) = {
		//		println(s"$datetime == ${other.datetime}; $duration == ${other.duration}; ${client.name} == ${other.client.name}")
		//		println(s"${datetime == other.datetime} && ${duration == other.duration} && ${client.name == other.client.name}")
		datetime == other.datetime && duration == other.duration && client.name == other.client.name
	}
}